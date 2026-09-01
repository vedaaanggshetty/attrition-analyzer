package com.example.EmployeeService.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import feign.FeignException;

/**
 * Integration test — requires the real Survey API Docker container running at
 * http://localhost:3232 (docker run -p 3232:3232 --name surveycontainer -d stackroutenew/surveyapi).
 * Named with the "IT" suffix so `mvn test` (Surefire) does NOT run it by default;
 * run explicitly with the container up to verify live connectivity.
 */
@SpringBootTest
class SurveyApiClientIT {

	@Autowired
	private SurveyApiClient surveyApiClient;

	@Test
	void fetchesRealEmployeeRecordsFromRunningSurveyApi() {
		List<SurveyEmployeeResponse> employees = surveyApiClient.getAllEmployees();

		assertThat(employees).isNotEmpty();
		assertThat(employees.get(0).EmployeeID()).isNotBlank();
	}

	@Test
	void filtersByDepartmentAgainstRunningSurveyApi() {
		List<SurveyEmployeeResponse> employees = surveyApiClient.findByProperty(Map.of("Department", "Sales"));

		assertThat(employees).isNotEmpty();
		assertThat(employees).allSatisfy(e -> assertThat(e.Department()).isEqualTo("Sales"));
	}

	@Test
	void returns404ForUnknownEmployeeIdAgainstRunningSurveyApi() {
		assertThatThrownBy(() -> surveyApiClient.getEmployeeById("does-not-exist"))
				.isInstanceOf(FeignException.NotFound.class);
	}
}
