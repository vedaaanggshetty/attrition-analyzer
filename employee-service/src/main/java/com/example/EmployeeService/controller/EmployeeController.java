package com.example.EmployeeService.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import com.example.EmployeeService.dto.EmployeeDto;
import com.example.EmployeeService.dto.ErrorResponse;
import com.example.EmployeeService.dto.FlagEmployeeRequest;
import com.example.EmployeeService.event.EmployeeFlaggedEvent;
import com.example.EmployeeService.exception.UnauthenticatedException;
import com.example.EmployeeService.security.JwtService;
import com.example.EmployeeService.service.EmployeeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * This service has no Spring Security of its own - every endpoint here is
 * reachable only through the Gateway's own JWT check, except the two
 * Guest-visible analysis endpoints the Gateway explicitly permits without a
 * token. See api-gateway's SecurityConfig for the actual enforcement.
 */
@Tag(name = "Employees", description = "Employee data (proxied from the external Survey API), search, and flagging")
@RestController
public class EmployeeController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final EmployeeService employeeService;
	private final JwtService jwtService;

	public EmployeeController(EmployeeService employeeService, JwtService jwtService) {
		this.employeeService = employeeService;
		this.jwtService = jwtService;
	}

	@Operation(
			summary = "List employees, or search by a single field",
			description = "With no query parameters, returns every employee. With both 'property' and 'value' "
					+ "given, returns only employees whose field matches (single-field search)."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Employees found",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							array = @ArraySchema(schema = @Schema(implementation = EmployeeDto.class)))),
			@ApiResponse(responseCode = "400", description = "Only one of 'property'/'value' was given",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "503", description = "The external Survey API is unavailable", content = @Content)
	})
	@GetMapping("/employees")
	public ResponseEntity<?> getEmployees(
			@Parameter(description = "Employee field to search on, e.g. \"department\"") @RequestParam(required = false) String property,
			@Parameter(description = "Value to match against that field, e.g. \"Sales\"") @RequestParam(required = false) String value) {

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

	@Operation(summary = "Get one employee by id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Employee found",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = EmployeeDto.class))),
			@ApiResponse(responseCode = "404", description = "No employee with this id",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping("/employees/{id}")
	public ResponseEntity<?> getEmployee(
			@Parameter(description = "Employee id", required = true) @PathVariable String id) {
		return employeeService.getEmployeeById(id)
				.<ResponseEntity<?>>map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new ErrorResponse("Employee not found")));
	}

	@Operation(
			summary = "Flag an employee for HR follow-up",
			description = "Publishes an EmployeeFlaggedEvent to Kafka for Notification Service to record - this "
					+ "call returns as soon as the event is published, it does not wait for the notification to "
					+ "actually be created.",
			security = @SecurityRequirement(name = "bearerAuth")
	)
	@ApiResponses({
			@ApiResponse(responseCode = "202", description = "Flag event published",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = EmployeeFlaggedEvent.class))),
			@ApiResponse(responseCode = "404", description = "No employee with this id",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token", content = @Content),
			@ApiResponse(responseCode = "503", description = "Failed to publish the event to Kafka", content = @Content)
	})
	@PostMapping("/employees/{id}/flag")
	public ResponseEntity<?> flagEmployee(
			@Parameter(description = "Employee id", required = true) @PathVariable String id,
			@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "Comment and optional sender name", required = true)
			@Valid @RequestBody FlagEmployeeRequest request) {
		String hrUserEmail = currentUserEmail(authorization);

		Optional<EmployeeFlaggedEvent> event = employeeService.flagEmployee(id, request.comment(), hrUserEmail, request.hrUserName());
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

	@Operation(summary = "Attrition rate by department",
			description = "Guest-visible - the Gateway permits this endpoint without a token.")
	@ApiResponse(responseCode = "200", description = "One row per department",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
					array = @ArraySchema(schema = @Schema(implementation = AttritionAnalysisDto.class))))
	@GetMapping("/employees/analysis/department")
	public List<AttritionAnalysisDto> getAttritionByDepartment() {
		return employeeService.getAttritionByDepartment();
	}

	@Operation(summary = "Attrition rate by job role",
			description = "Guest-visible - the Gateway permits this endpoint without a token.")
	@ApiResponse(responseCode = "200", description = "One row per job role",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
					array = @ArraySchema(schema = @Schema(implementation = AttritionAnalysisDto.class))))
	@GetMapping("/employees/analysis/job-role")
	public List<AttritionAnalysisDto> getAttritionByJobRole() {
		return employeeService.getAttritionByJobRole();
	}

	@Operation(summary = "Attrition rate by compensation band",
			security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "One row per compensation band",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
					array = @ArraySchema(schema = @Schema(implementation = AttritionAnalysisDto.class))))
	@GetMapping("/employees/analysis/compensation")
	public List<AttritionAnalysisDto> getAttritionByCompensation() {
		return employeeService.getAttritionByCompensation();
	}

	@Operation(summary = "Attrition rate by gender",
			security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "One row per demographic group",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
					array = @ArraySchema(schema = @Schema(implementation = AttritionAnalysisDto.class))))
	@GetMapping("/employees/analysis/demographics")
	public List<AttritionAnalysisDto> getAttritionByDemographics() {
		return employeeService.getAttritionByDemographics();
	}

	@Operation(summary = "Attrition rate by overtime status",
			security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "One row per overtime status",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
					array = @ArraySchema(schema = @Schema(implementation = AttritionAnalysisDto.class))))
	@GetMapping("/employees/analysis/work-life-balance")
	public List<AttritionAnalysisDto> getAttritionByWorkLifeBalance() {
		return employeeService.getAttritionByWorkLifeBalance();
	}

	@Operation(summary = "Attrition rate by promotion recency",
			security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "One row per promotion band",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
					array = @ArraySchema(schema = @Schema(implementation = AttritionAnalysisDto.class))))
	@GetMapping("/employees/analysis/career-progression")
	public List<AttritionAnalysisDto> getAttritionByCareerProgression() {
		return employeeService.getAttritionByCareerProgression();
	}
}
