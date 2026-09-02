package com.example.EmployeeService.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import feign.FeignException;

// Needs the real Survey API container running on localhost:3232.
// Named *IT so plain `mvn test` skips it - run this one explicitly.
@SpringBootTest
class SurveyApiClientIT {

	@Autowired
	private SurveyApiClient surveyApiClient;

	@Test
	void fetchesRealEmployees() {
		List<SurveyEmployeeResponse> employees = surveyApiClient.getAllEmployees();

		assertThat(employees).isNotEmpty();
	}

	@Test
	void filtersByDepartment() {
		List<SurveyEmployeeResponse> employees = surveyApiClient.findByProperty(Map.of("Department", "Sales"));

		assertThat(employees).isNotEmpty();
		assertThat(employees).allSatisfy(e -> assertThat(e.Department()).isEqualTo("Sales"));
	}

	@Test
	void unknownIdReturns404() {
		assertThatThrownBy(() -> surveyApiClient.getEmployeeById("does-not-exist"))
				.isInstanceOf(FeignException.NotFound.class);
	}
}
