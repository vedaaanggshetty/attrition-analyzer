package com.example.EmployeeService.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
import com.example.EmployeeService.dto.EmployeeDto;
import com.example.EmployeeService.exception.SurveyApiException;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	@Mock
	private SurveyApiClient surveyApiClient;

	private EmployeeService employeeService;

	@BeforeEach
	void setUp() {
		employeeService = new EmployeeService(surveyApiClient);
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
}
