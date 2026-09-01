package com.example.EmployeeService.dto;

public record EmployeeDto(
		String id,
		String employeeId,
		String firstName,
		String lastName,
		String gender,
		Integer age,
		String businessTravel,
		String department,
		Integer distanceFromHomeKm,
		String state,
		String ethnicity,
		Integer education,
		String educationField,
		String jobRole,
		String maritalStatus,
		Integer salary,
		Integer stockOptionLevel,
		String overTime,
		String hireDate,
		String attrition,
		Integer yearsAtCompany,
		Integer yearsInMostRecentRole,
		Integer yearsSinceLastPromotion,
		Integer yearsWithCurrManager) {
}
