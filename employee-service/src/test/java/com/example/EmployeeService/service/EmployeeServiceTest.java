package com.example.EmployeeService.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import feign.FeignException;
import feign.Request;
import feign.Response;

import com.example.EmployeeService.client.SurveyApiClient;
import com.example.EmployeeService.client.SurveyEmployeeResponse;
import com.example.EmployeeService.dto.EmployeeDto;
import com.example.EmployeeService.exception.SurveyApiException;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	@Mock
	private SurveyApiClient surveyApiClient;

	private EmployeeService employeeService;

	private static SurveyEmployeeResponse sampleResponse() {
		return new SurveyEmployeeResponse(
				"3012-1A41", "Leonelle", "Simco", "Female", 30, "Some Travel", "Sales", 27,
				"IL", "White", 5, "Marketing", "Sales Executive", "Divorced", 102059, 1,
				"No", "2012-01-03", "No", 10, 4, 9, 7, "5a94");
	}

	private static FeignException feignExceptionWithStatus(int status) {
		Request request = Request.create(Request.HttpMethod.GET, "/survey/does-not-exist",
				Map.of(), (byte[]) null, StandardCharsets.UTF_8);
		Response response = Response.builder()
				.status(status)
				.reason("error")
				.request(request)
				.headers(Map.of())
				.build();
		return FeignException.errorStatus("SurveyApiClient#call()", response);
	}

	@Test
	void getAllEmployeesMapsSuccessfulSurveyApiResponse() {
		employeeService = new EmployeeService(surveyApiClient);
		given(surveyApiClient.getAllEmployees()).willReturn(List.of(sampleResponse()));

		List<EmployeeDto> employees = employeeService.getAllEmployees();

		assertThat(employees).hasSize(1);
		assertThat(employees.get(0).employeeId()).isEqualTo("3012-1A41");
		assertThat(employees.get(0).department()).isEqualTo("Sales");
	}

	@Test
	void findByPropertyReturnsEmptyListWhenSurveyApiHasNoMatch() {
		employeeService = new EmployeeService(surveyApiClient);
		given(surveyApiClient.findByProperty(any())).willReturn(List.of());

		List<EmployeeDto> employees = employeeService.findByProperty("Department", "NoSuchDept");

		assertThat(employees).isEmpty();
	}

	@Test
	void getEmployeeByIdReturnsEmptyOptionalWhenSurveyApiReturns404() {
		employeeService = new EmployeeService(surveyApiClient);
		given(surveyApiClient.getEmployeeById("missing")).willThrow(feignExceptionWithStatus(404));

		Optional<EmployeeDto> employee = employeeService.getEmployeeById("missing");

		assertThat(employee).isEmpty();
	}

	@Test
	void getAllEmployeesWrapsOtherSurveyApiFailuresInSurveyApiException() {
		employeeService = new EmployeeService(surveyApiClient);
		given(surveyApiClient.getAllEmployees()).willThrow(feignExceptionWithStatus(500));

		assertThatThrownBy(() -> employeeService.getAllEmployees())
				.isInstanceOf(SurveyApiException.class)
				.hasCauseInstanceOf(FeignException.class);
	}
}
