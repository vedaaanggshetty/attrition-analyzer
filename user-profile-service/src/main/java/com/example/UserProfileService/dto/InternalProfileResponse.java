package com.example.UserProfileService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response body for the internal profile-lookup endpoint. Intentionally
 * exposes only {@code fullName} (not email/phone) - the only other services
 * calling this need is resolving a display name for a userId, same
 * least-privilege reasoning as {@code RegisterCredentialResponse} in
 * Authentication Service exposing only what the caller actually needs.
 */
@Schema(description = "A user's display name, for service-to-service lookups by userId")
public record InternalProfileResponse(

        @Schema(description = "The looked-up user's id")
        UUID userId,

        @Schema(description = "Display name", example = "Jordan Lee")
        String fullName) {
}
