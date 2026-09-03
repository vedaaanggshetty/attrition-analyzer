package com.example.EmployeeService.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.EmployeeService.dto.AttritionAnalysisDto;
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
	void getEmployeesReturnsList() throws Exception {
		given(employeeService.getAllEmployees()).willReturn(List.of(sampleEmployeeDto()));

		mockMvc.perform(get("/employees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].department").value("Sales"));
	}

	@Test
	void getEmployeesReturns503WhenSurveyApiIsDown() throws Exception {
		given(employeeService.getAllEmployees())
				.willThrow(new SurveyApiException("Survey API down", new RuntimeException()));

		mockMvc.perform(get("/employees"))
				.andExpect(status().isServiceUnavailable());
	}

	@Test
	void searchReturnsMatches() throws Exception {
		given(employeeService.findByProperty("Department", "Sales")).willReturn(List.of(sampleEmployeeDto()));

		mockMvc.perform(get("/employees").param("property", "Department").param("value", "Sales"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].department").value("Sales"));
	}

	@Test
	void searchReturnsEmptyArrayWhenNothingMatches() throws Exception {
		given(employeeService.findByProperty("Department", "NoSuchDept")).willReturn(List.of());

		mockMvc.perform(get("/employees").param("property", "Department").param("value", "NoSuchDept"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void searchWithoutValueReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/employees").param("property", "Department"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getEmployeeReturnsDetailsWhenFound() throws Exception {
		given(employeeService.getEmployeeById("5a94")).willReturn(Optional.of(sampleEmployeeDto()));

		mockMvc.perform(get("/employees/5a94"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Leonelle"));
	}

	@Test
	void getEmployeeReturns404WhenNotFound() throws Exception {
		given(employeeService.getEmployeeById("missing")).willReturn(Optional.empty());

		mockMvc.perform(get("/employees/missing"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getAttritionByDepartmentReturnsAggregatedResults() throws Exception {
		given(employeeService.getAttritionByDepartment()).willReturn(List.of(
				new AttritionAnalysisDto("Sales", 4, 2, 50.0)));

		mockMvc.perform(get("/employees/analysis/department"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].groupLabel").value("Sales"))
				.andExpect(jsonPath("$[0].totalEmployees").value(4))
				.andExpect(jsonPath("$[0].attritionCount").value(2))
				.andExpect(jsonPath("$[0].attritionRate").value(50.0));
	}

	@Test
	void getAttritionByDepartmentReturns503WhenSurveyApiIsDown() throws Exception {
		given(employeeService.getAttritionByDepartment())
				.willThrow(new SurveyApiException("Survey API down", new RuntimeException()));

		mockMvc.perform(get("/employees/analysis/department"))
				.andExpect(status().isServiceUnavailable());
	}
}
