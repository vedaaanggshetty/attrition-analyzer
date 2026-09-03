package com.example.EmployeeService.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import feign.FeignException;
import feign.Request;
import feign.Response;

import com.example.EmployeeService.client.SurveyApiClient;
import com.example.EmployeeService.client.SurveyEmployeeResponse;
import com.example.EmployeeService.dto.AttritionAnalysisDto;
import com.example.EmployeeService.dto.EmployeeDto;
import com.example.EmployeeService.event.EmployeeFlaggedEventProducer;
import com.example.EmployeeService.exception.SurveyApiException;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	@Mock
	private SurveyApiClient surveyApiClient;

	@Mock
	private EmployeeFlaggedEventProducer eventProducer;

	private EmployeeService employeeService;

	@BeforeEach
	void setUp() {
		employeeService = new EmployeeService(surveyApiClient, eventProducer);
	}

	private static SurveyEmployeeResponse sampleResponse() {
		return new SurveyEmployeeResponse(
				"3012-1A41", "Leonelle", "Simco", "Female", 30, "Some Travel", "Sales", 27,
				"IL", "White", 5, "Marketing", "Sales Executive", "Divorced", 102059, 1,
				"No", "2012-01-03", "No", 10, 4, 9, 7, "5a94");
	}

	private static FeignException feignError(int status) {
		Request request = Request.create(Request.HttpMethod.GET, "/survey", Map.of(), null, StandardCharsets.UTF_8);
		Response response = Response.builder().status(status).request(request).headers(Map.of()).build();
		return FeignException.errorStatus("call", response);
	}

	private static SurveyEmployeeResponse responseWith(String department, String attrition) {
		return new SurveyEmployeeResponse(
				"3012-1A41", "Leonelle", "Simco", "Female", 30, "Some Travel", department, 27,
				"IL", "White", 5, "Marketing", "Sales Executive", "Divorced", 102059, 1,
				"No", "2012-01-03", attrition, 10, 4, 9, 7, "5a94");
	}

	private static SurveyEmployeeResponse response(String jobRole, String gender, Integer salary,
			String overTime, Integer yearsSincePromotion, String attrition) {
		return new SurveyEmployeeResponse(
				"3012-1A41", "Leonelle", "Simco", gender, 30, "Some Travel", "Sales", 27,
				"IL", "White", 5, "Marketing", jobRole, "Divorced", salary, 1,
				overTime, "2012-01-03", attrition, 10, 4, yearsSincePromotion, 7, "5a94");
	}

	@Test
	void getAllEmployeesReturnsMappedList() {
		given(surveyApiClient.getAllEmployees()).willReturn(List.of(sampleResponse()));

		List<EmployeeDto> employees = employeeService.getAllEmployees();

		assertThat(employees).hasSize(1);
		assertThat(employees.get(0).department()).isEqualTo("Sales");
	}

	@Test
	void findByPropertyReturnsEmptyListWhenNothingMatches() {
		given(surveyApiClient.findByProperty(any())).willReturn(List.of());

		assertThat(employeeService.findByProperty("Department", "NoSuchDept")).isEmpty();
	}

	@Test
	void getEmployeeByIdReturnsEmptyWhenNotFound() {
		given(surveyApiClient.getEmployeeById("missing")).willThrow(feignError(404));

		Optional<EmployeeDto> employee = employeeService.getEmployeeById("missing");

		assertThat(employee).isEmpty();
	}

	@Test
	void getAllEmployeesThrowsSurveyApiExceptionOnFailure() {
		given(surveyApiClient.getAllEmployees()).willThrow(feignError(500));

		assertThatThrownBy(() -> employeeService.getAllEmployees())
				.isInstanceOf(SurveyApiException.class);
	}

	@Test
	void flagEmployeePublishesEventWithExpectedContents() {
		given(surveyApiClient.getEmployeeById("5a94")).willReturn(sampleResponse());

		Optional<com.example.EmployeeService.event.EmployeeFlaggedEvent> result =
				employeeService.flagEmployee("5a94", "Watch closely", "hr@example.com");

		assertThat(result).isPresent();
		var event = result.get();
		assertThat(event.eventId()).isNotNull();
		assertThat(event.employeeId()).isEqualTo("3012-1A41");
		assertThat(event.employeeName()).isEqualTo("Leonelle Simco");
		assertThat(event.department()).isEqualTo("Sales");
		assertThat(event.comment()).isEqualTo("Watch closely");
		assertThat(event.hrUserEmail()).isEqualTo("hr@example.com");
		assertThat(event.flaggedAt()).isNotNull();

		then(eventProducer).should().publish(event);
	}

	@Test
	void flagEmployeeReturnsEmptyWhenEmployeeNotFound() {
		given(surveyApiClient.getEmployeeById("missing")).willThrow(feignError(404));

		Optional<com.example.EmployeeService.event.EmployeeFlaggedEvent> result =
				employeeService.flagEmployee("missing", "comment", "hr@example.com");

		assertThat(result).isEmpty();
		then(eventProducer).shouldHaveNoInteractions();
	}

	@Test
	void flagEmployeePropagatesEventPublicationFailure() {
		given(surveyApiClient.getEmployeeById("5a94")).willReturn(sampleResponse());
		willThrow(new com.example.EmployeeService.exception.EventPublicationException("Kafka down", new RuntimeException()))
				.given(eventProducer).publish(any());

		assertThatThrownBy(() -> employeeService.flagEmployee("5a94", "comment", "hr@example.com"))
				.isInstanceOf(com.example.EmployeeService.exception.EventPublicationException.class);
	}

	@Test
	void getAttritionByDepartmentGroupsAndCalculatesRateCorrectly() {
		given(surveyApiClient.getAllEmployees()).willReturn(List.of(
				responseWith("Sales", "Yes"),
				responseWith("Sales", "No"),
				responseWith("Sales", "No"),
				responseWith("Sales", "Yes"),
				responseWith("Technology", "No"),
				responseWith("Technology", "No")));

		List<AttritionAnalysisDto> result = employeeService.getAttritionByDepartment();

		assertThat(result).hasSize(2);

		AttritionAnalysisDto sales = result.get(0);
		assertThat(sales.groupLabel()).isEqualTo("Sales");
		assertThat(sales.totalEmployees()).isEqualTo(4);
		assertThat(sales.attritionCount()).isEqualTo(2);
		assertThat(sales.attritionRate()).isEqualTo(50.0);

		AttritionAnalysisDto technology = result.get(1);
		assertThat(technology.groupLabel()).isEqualTo("Technology");
		assertThat(technology.totalEmployees()).isEqualTo(2);
		assertThat(technology.attritionCount()).isEqualTo(0);
		assertThat(technology.attritionRate()).isEqualTo(0.0);
	}

	@Test
	void getAttritionByDepartmentReturnsEmptyListWhenNoEmployees() {
		given(surveyApiClient.getAllEmployees()).willReturn(List.of());

		assertThat(employeeService.getAttritionByDepartment()).isEmpty();
	}

	@Test
	void getAttritionByDepartmentGroupsBlankDepartmentAsUnknown() {
		given(surveyApiClient.getAllEmployees()).willReturn(List.of(responseWith("", "Yes")));

		List<AttritionAnalysisDto> result = employeeService.getAttritionByDepartment();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).groupLabel()).isEqualTo("Unknown");
	}

	@Test
	void getAttritionByDepartmentThrowsSurveyApiExceptionOnFailure() {
		given(surveyApiClient.getAllEmployees()).willThrow(feignError(500));

		assertThatThrownBy(() -> employeeService.getAttritionByDepartment())
				.isInstanceOf(SurveyApiException.class);
	}

	@Test
	void getAttritionByJobRoleGroupsCorrectly() {
		given(surveyApiClient.getAllEmployees()).willReturn(List.of(
				response("Sales Executive", "Female", 100000, "No", 1, "Yes"),
				response("Sales Executive", "Male", 100000, "No", 1, "No"),
				response("HR Business Partner", "Female", 90000, "No", 1, "No")));

		List<AttritionAnalysisDto> result = employeeService.getAttritionByJobRole();

		assertThat(result).hasSize(2);
		AttritionAnalysisDto hr = result.get(0);
		assertThat(hr.groupLabel()).isEqualTo("HR Business Partner");
		assertThat(hr.totalEmployees()).isEqualTo(1);
		assertThat(hr.attritionCount()).isEqualTo(0);
		AttritionAnalysisDto sales = result.get(1);
		assertThat(sales.groupLabel()).isEqualTo("Sales Executive");
		assertThat(sales.totalEmployees()).isEqualTo(2);
		assertThat(sales.attritionCount()).isEqualTo(1);
		assertThat(sales.attritionRate()).isEqualTo(50.0);
	}

	@Test
	void getAttritionByCompensationBucketsSalaryIntoFiftyThousandBands() {
		given(surveyApiClient.getAllEmployees()).willReturn(List.of(
				response("Sales Executive", "Female", 40000, "No", 1, "Yes"),
				response("Sales Executive", "Male", 45000, "No", 1, "No"),
				response("Sales Executive", "Male", 120000, "No", 1, "No")));

		List<AttritionAnalysisDto> result = employeeService.getAttritionByCompensation();

		assertThat(result).hasSize(2);
		assertThat(result.get(0).groupLabel()).isEqualTo("$0-$49999");
		assertThat(result.get(0).totalEmployees()).isEqualTo(2);
		assertThat(result.get(0).attritionCount()).isEqualTo(1);
		assertThat(result.get(1).groupLabel()).isEqualTo("$100000-$149999");
		assertThat(result.get(1).totalEmployees()).isEqualTo(1);
	}

	@Test
	void getAttritionByCompensationTreatsNullSalaryAsUnknown() {
		given(surveyApiClient.getAllEmployees()).willReturn(List.of(
				response("Sales Executive", "Female", null, "No", 1, "No")));

		List<AttritionAnalysisDto> result = employeeService.getAttritionByCompensation();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).groupLabel()).isEqualTo("Unknown");
	}

	@Test
	void getAttritionByDemographicsGroupsByGender() {
		given(surveyApiClient.getAllEmployees()).willReturn(List.of(
				response("Sales Executive", "Female", 100000, "No", 1, "Yes"),
				response("Sales Executive", "Male", 100000, "No", 1, "No")));

		List<AttritionAnalysisDto> result = employeeService.getAttritionByDemographics();

		assertThat(result).hasSize(2);
		assertThat(result.get(0).groupLabel()).isEqualTo("Female");
		assertThat(result.get(1).groupLabel()).isEqualTo("Male");
	}

	@Test
	void getAttritionByWorkLifeBalanceGroupsByOverTime() {
		given(surveyApiClient.getAllEmployees()).willReturn(List.of(
				response("Sales Executive", "Female", 100000, "Yes", 1, "Yes"),
				response("Sales Executive", "Male", 100000, "Yes", 1, "Yes"),
				response("Sales Executive", "Male", 100000, "No", 1, "No")));

		List<AttritionAnalysisDto> result = employeeService.getAttritionByWorkLifeBalance();

		assertThat(result).hasSize(2);
		assertThat(result.get(0).groupLabel()).isEqualTo("No");
		assertThat(result.get(0).attritionCount()).isEqualTo(0);
		assertThat(result.get(1).groupLabel()).isEqualTo("Yes");
		assertThat(result.get(1).totalEmployees()).isEqualTo(2);
		assertThat(result.get(1).attritionCount()).isEqualTo(2);
		assertThat(result.get(1).attritionRate()).isEqualTo(100.0);
	}

	@Test
	void getAttritionByCareerProgressionBucketsYearsSincePromotion() {
		given(surveyApiClient.getAllEmployees()).willReturn(List.of(
				response("Sales Executive", "Female", 100000, "No", 1, "No"),
				response("Sales Executive", "Male", 100000, "No", 4, "No"),
				response("Sales Executive", "Male", 100000, "No", 8, "Yes")));

		List<AttritionAnalysisDto> result = employeeService.getAttritionByCareerProgression();

		assertThat(result).hasSize(3);
		assertThat(result.get(0).groupLabel()).isEqualTo("0-2 years");
		assertThat(result.get(1).groupLabel()).isEqualTo("3-5 years");
		assertThat(result.get(2).groupLabel()).isEqualTo("6+ years");
		assertThat(result.get(2).attritionCount()).isEqualTo(1);
	}

	@Test
	void getAttritionByCareerProgressionTreatsNullYearsAsUnknown() {
		given(surveyApiClient.getAllEmployees()).willReturn(List.of(
				response("Sales Executive", "Female", 100000, "No", null, "No")));

		List<AttritionAnalysisDto> result = employeeService.getAttritionByCareerProgression();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).groupLabel()).isEqualTo("Unknown");
	}
}
