package com.example.EmployeeService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FlagEmployeeRequest(

		@NotBlank(message = "Comment is required")
		@Size(max = 1000, message = "Comment must be at most 1000 characters")
		String comment) {
}
