package com.example.EmployeeService.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.EmployeeService.dto.AttritionAnalysisDto;
import com.example.EmployeeService.dto.ErrorResponse;
import com.example.EmployeeService.dto.FlagEmployeeRequest;
import com.example.EmployeeService.event.EmployeeFlaggedEvent;
import com.example.EmployeeService.exception.UnauthenticatedException;
import com.example.EmployeeService.security.JwtService;
import com.example.EmployeeService.service.EmployeeService;

import jakarta.validation.Valid;

@RestController
public class EmployeeController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final EmployeeService employeeService;
	private final JwtService jwtService;

	public EmployeeController(EmployeeService employeeService, JwtService jwtService) {
		this.employeeService = employeeService;
		this.jwtService = jwtService;
	}

	@GetMapping("/employees")
	public ResponseEntity<?> getEmployees(
			@RequestParam(required = false) String property,
			@RequestParam(required = false) String value) {

		boolean hasProperty = StringUtils.hasText(property);
		boolean hasValue = StringUtils.hasText(value);

		if (!hasProperty && !hasValue) {
			return ResponseEntity.ok(employeeService.getAllEmployees());
		}

		if (hasProperty && hasValue) {
			return ResponseEntity.ok(employeeService.findByProperty(property, value));
		}

		return ResponseEntity.badRequest()
				.body(new ErrorResponse("Both 'property' and 'value' query parameters are required for search"));
	}

	@GetMapping("/employees/{id}")
	public ResponseEntity<?> getEmployee(@PathVariable String id) {
		return employeeService.getEmployeeById(id)
				.<ResponseEntity<?>>map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new ErrorResponse("Employee not found")));
	}

	@PostMapping("/employees/{id}/flag")
	public ResponseEntity<?> flagEmployee(@PathVariable String id,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody FlagEmployeeRequest request) {
		String hrUserEmail = currentUserEmail(authorization);

		Optional<EmployeeFlaggedEvent> event = employeeService.flagEmployee(id, request.comment(), hrUserEmail);
		return event
				.<ResponseEntity<?>>map(e -> ResponseEntity.status(HttpStatus.ACCEPTED).body(e))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new ErrorResponse("Employee not found")));
	}

	private String currentUserEmail(String authorization) {
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			throw new UnauthenticatedException();
		}
		return jwtService.extractEmail(authorization.substring(BEARER_PREFIX.length()));
	}

	@GetMapping("/employees/analysis/department")
	public List<AttritionAnalysisDto> getAttritionByDepartment() {
		return employeeService.getAttritionByDepartment();
	}

	@GetMapping("/employees/analysis/job-role")
	public List<AttritionAnalysisDto> getAttritionByJobRole() {
		return employeeService.getAttritionByJobRole();
	}

	@GetMapping("/employees/analysis/compensation")
	public List<AttritionAnalysisDto> getAttritionByCompensation() {
		return employeeService.getAttritionByCompensation();
	}

	@GetMapping("/employees/analysis/demographics")
	public List<AttritionAnalysisDto> getAttritionByDemographics() {
		return employeeService.getAttritionByDemographics();
	}

	@GetMapping("/employees/analysis/work-life-balance")
	public List<AttritionAnalysisDto> getAttritionByWorkLifeBalance() {
		return employeeService.getAttritionByWorkLifeBalance();
	}

	@GetMapping("/employees/analysis/career-progression")
	public List<AttritionAnalysisDto> getAttritionByCareerProgression() {
		return employeeService.getAttritionByCareerProgression();
	}
}
