package com.example.EmployeeService.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.EmployeeService.dto.EmployeeDto;
import com.example.EmployeeService.exception.SurveyApiException;
import com.example.EmployeeService.service.EmployeeService;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmployeeService employeeService;

	private static EmployeeDto sampleEmployeeDto() {
		return new EmployeeDto(
				"5a94", "3012-1A41", "Leonelle", "Simco", "Female", 30, "Some Travel", "Sales", 27,
				"IL", "White", 5, "Marketing", "Sales Executive", "Divorced", 102059, 1,
				"No", "2012-01-03", "No", 10, 4, 9, 7);
	}

	@Test
	void getEmployeesReturnsEmployeeRecordsOnSuccess() throws Exception {
		given(employeeService.getAllEmployees()).willReturn(List.of(sampleEmployeeDto()));

		mockMvc.perform(get("/employees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].employeeId").value("3012-1A41"))
				.andExpect(jsonPath("$[0].department").value("Sales"))
				.andExpect(jsonPath("$[0].jobRole").value("Sales Executive"));
	}

	@Test
	void getEmployeesReturnsServiceUnavailableWhenSurveyApiFails() throws Exception {
		given(employeeService.getAllEmployees())
				.willThrow(new SurveyApiException("Failed to retrieve employees from Survey API", new RuntimeException()));

		mockMvc.perform(get("/employees"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value("Failed to retrieve employees from Survey API"));
	}
}
