package com.example.AuthService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standard error body returned by every failed request")
public record ErrorResponse(

        @Schema(description = "When the error occurred")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "401")
        int status,

        @Schema(description = "HTTP status reason phrase", example = "Unauthorized")
        String error,

        @Schema(description = "Human-readable explanation of what went wrong", example = "Invalid email or password")
        String message
) {
}
