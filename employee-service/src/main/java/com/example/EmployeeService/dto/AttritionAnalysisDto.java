package com.example.EmployeeService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Attrition rate for one group (e.g. one department, one job role)")
public record AttritionAnalysisDto(

		@Schema(description = "The group this row summarizes", example = "Sales")
		String groupLabel,

		@Schema(description = "Total employees in this group", example = "446")
		int totalEmployees,

		@Schema(description = "How many of them have attrition = Yes", example = "92")
		int attritionCount,

		@Schema(description = "attritionCount / totalEmployees, as a percentage", example = "20.63")
		double attritionRate) {
}
