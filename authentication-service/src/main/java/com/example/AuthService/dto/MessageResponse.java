package com.example.AuthService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generic success message body, used for endpoints (logout, password reset)
 * that don't need to return a domain object.
 */
@Schema(description = "Generic success message")
public record MessageResponse(

        @Schema(description = "Human-readable confirmation of what happened", example = "Logged out successfully")
        String message) {
}
