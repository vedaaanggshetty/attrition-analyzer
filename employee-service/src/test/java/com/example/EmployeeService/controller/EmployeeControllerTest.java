package com.example.EmployeeService.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.EmployeeService.dto.AttritionAnalysisDto;
import com.example.EmployeeService.dto.EmployeeDto;
import com.example.EmployeeService.event.EmployeeFlaggedEvent;
import com.example.EmployeeService.exception.SurveyApiException;
import com.example.EmployeeService.security.JwtService;
import com.example.EmployeeService.service.EmployeeService;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmployeeService employeeService;

	@MockitoBean
	private JwtService jwtService;

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
	void flagEmployeeReturns202WhenFound() throws Exception {
		given(jwtService.extractEmail("token")).willReturn("hr@example.com");
		EmployeeFlaggedEvent event = new EmployeeFlaggedEvent(
				UUID.randomUUID(), "3012-1A41", "Leonelle Simco", "Sales", "Watch closely",
				"hr@example.com", Instant.now());
		given(employeeService.flagEmployee("5a94", "Watch closely", "hr@example.com"))
				.willReturn(Optional.of(event));

		mockMvc.perform(post("/employees/5a94/flag")
						.header("Authorization", "Bearer token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"comment\":\"Watch closely\"}"))
				.andExpect(status().isAccepted());
	}

	@Test
	void flagEmployeeReturns404WhenNotFound() throws Exception {
		given(jwtService.extractEmail("token")).willReturn("hr@example.com");
		given(employeeService.flagEmployee("missing", "Watch closely", "hr@example.com"))
				.willReturn(Optional.empty());

		mockMvc.perform(post("/employees/missing/flag")
						.header("Authorization", "Bearer token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"comment\":\"Watch closely\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void flagEmployeeReturns401WhenNoAuthorizationHeader() throws Exception {
		mockMvc.perform(post("/employees/5a94/flag")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"comment\":\"Watch closely\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void flagEmployeeReturns503WhenKafkaPublicationFails() throws Exception {
		given(jwtService.extractEmail("token")).willReturn("hr@example.com");
		given(employeeService.flagEmployee("5a94", "Watch closely", "hr@example.com"))
				.willThrow(new com.example.EmployeeService.exception.EventPublicationException(
						"Failed to publish EmployeeFlaggedEvent to Kafka", new RuntimeException()));

		mockMvc.perform(post("/employees/5a94/flag")
						.header("Authorization", "Bearer token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"comment\":\"Watch closely\"}"))
				.andExpect(status().isServiceUnavailable());
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

	@Test
	void getAttritionByJobRoleReturnsAggregatedResults() throws Exception {
		given(employeeService.getAttritionByJobRole()).willReturn(List.of(
				new AttritionAnalysisDto("Sales Executive", 2, 1, 50.0)));

		mockMvc.perform(get("/employees/analysis/job-role"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].groupLabel").value("Sales Executive"));
	}

	@Test
	void getAttritionByCompensationReturnsAggregatedResults() throws Exception {
		given(employeeService.getAttritionByCompensation()).willReturn(List.of(
				new AttritionAnalysisDto("$50000-$99999", 3, 1, 33.33)));

		mockMvc.perform(get("/employees/analysis/compensation"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].groupLabel").value("$50000-$99999"));
	}

	@Test
	void getAttritionByDemographicsReturnsAggregatedResults() throws Exception {
		given(employeeService.getAttritionByDemographics()).willReturn(List.of(
				new AttritionAnalysisDto("Female", 5, 1, 20.0)));

		mockMvc.perform(get("/employees/analysis/demographics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].groupLabel").value("Female"));
	}

	@Test
	void getAttritionByWorkLifeBalanceReturnsAggregatedResults() throws Exception {
		given(employeeService.getAttritionByWorkLifeBalance()).willReturn(List.of(
				new AttritionAnalysisDto("Yes", 2, 2, 100.0)));

		mockMvc.perform(get("/employees/analysis/work-life-balance"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].groupLabel").value("Yes"));
	}

	@Test
	void getAttritionByCareerProgressionReturnsAggregatedResults() throws Exception {
		given(employeeService.getAttritionByCareerProgression()).willReturn(List.of(
				new AttritionAnalysisDto("0-2 years", 1, 0, 0.0)));

		mockMvc.perform(get("/employees/analysis/career-progression"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].groupLabel").value("0-2 years"));
	}
}
