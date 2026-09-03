package com.example.EmployeeService.dto;

public record AttritionAnalysisDto(
		String groupLabel,
		int totalEmployees,
		int attritionCount,
		double attritionRate) {
}
