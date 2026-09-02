package com.example.AuthService.dto;

import java.util.UUID;

/**
 * Response body for the internal credential-registration endpoint.
 * Intentionally exposes only the generated {@code userId} - never the
 * password hash or any other credential detail.
 */
public record RegisterCredentialResponse(UUID userId) {
}
