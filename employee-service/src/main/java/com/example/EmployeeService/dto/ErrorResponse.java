package com.example.EmployeeService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simple error body returned by this service")
public record ErrorResponse(

		@Schema(description = "Human-readable explanation of what went wrong", example = "Employee not found")
		String message) {
}
