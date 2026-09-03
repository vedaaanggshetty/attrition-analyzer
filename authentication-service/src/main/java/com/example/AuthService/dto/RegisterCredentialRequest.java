package com.example.AuthService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the internal credential-registration endpoint
 * ({@code POST /internal/credentials}). Not the frontend-facing
 * registration contract - that lives on User Profile Service.
 */
public record RegisterCredentialRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
