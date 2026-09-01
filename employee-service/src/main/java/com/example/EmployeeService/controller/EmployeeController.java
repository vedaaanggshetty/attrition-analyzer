package com.example.EmployeeService.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.EmployeeService.dto.EmployeeDto;
import com.example.EmployeeService.service.EmployeeService;

@RestController
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	@GetMapping("/employees")
	public List<EmployeeDto> getEmployees() {
		return employeeService.getAllEmployees();
	}
}
