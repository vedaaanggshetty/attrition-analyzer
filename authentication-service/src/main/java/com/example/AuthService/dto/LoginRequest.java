package com.example.AuthService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials submitted to log in")
public record LoginRequest(

        @Schema(description = "Registered HR email address", example = "hr@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Account password", example = "Password123!")
        @NotBlank(message = "Password is required")
        String password
) {
}
