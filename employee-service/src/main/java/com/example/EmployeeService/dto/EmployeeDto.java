package com.example.EmployeeService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One employee record, proxied and mapped from the external Survey API - never the raw Survey API shape")
public record EmployeeDto(
		@Schema(description = "Internal identifier used in URLs (/employees/{id})") String id,
		@Schema(description = "Business-facing employee id from the Survey API", example = "3012-1A41") String employeeId,
		String firstName,
		String lastName,
		String gender,
		Integer age,
		String businessTravel,
		@Schema(example = "Sales") String department,
		Integer distanceFromHomeKm,
		String state,
		String ethnicity,
		Integer education,
		String educationField,
		String jobRole,
		String maritalStatus,
		Integer salary,
		Integer stockOptionLevel,
		@Schema(description = "\"Yes\" or \"No\"", example = "No") String overTime,
		String hireDate,
		@Schema(description = "\"Yes\" or \"No\" - whether this employee has left", example = "No") String attrition,
		Integer yearsAtCompany,
		Integer yearsInMostRecentRole,
		Integer yearsSinceLastPromotion,
		Integer yearsWithCurrManager) {
}
