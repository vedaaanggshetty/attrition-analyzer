package com.example.AuthService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the internal credential-registration endpoint
 * ({@code POST /internal/credentials}). Not the frontend-facing
 * registration contract - that lives on User Profile Service.
 */
@Schema(description = "Internal, service-to-service request to create a new credential")
public record RegisterCredentialRequest(

        @Schema(description = "Email address for the new credential", example = "new-hr@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Plaintext password - hashed before storage, never persisted as-is")
        @NotBlank(message = "Password is required")
        String password
) {
}
