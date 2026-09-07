package com.example.AuthService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response body for the internal credential-registration endpoint.
 * Intentionally exposes only the generated {@code userId} - never the
 * password hash or any other credential detail.
 */
@Schema(description = "Result of creating a new credential")
public record RegisterCredentialResponse(

        @Schema(description = "Generated identity shared with User Profile Service")
        UUID userId) {
}
