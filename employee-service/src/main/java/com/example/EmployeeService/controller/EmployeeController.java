package com.example.EmployeeService.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.EmployeeService.dto.AttritionAnalysisDto;
import com.example.EmployeeService.dto.ErrorResponse;
import com.example.EmployeeService.service.EmployeeService;

@RestController
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
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

	@GetMapping("/employees/analysis/department")
	public List<AttritionAnalysisDto> getAttritionByDepartment() {
		return employeeService.getAttritionByDepartment();
	}
}
