package com.example.UserProfileService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body for {@code GET /users/me} / {@code PUT /users/me}.
 */
@Schema(description = "The caller's own HR profile")
public record ProfileResponse(

        @Schema(description = "Identity shared with Authentication Service")
        UUID userId,

        @Schema(description = "Display name", example = "Jordan Lee")
        String fullName,

        @Schema(description = "Registered email address", example = "hr@example.com")
        String email,

        @Schema(description = "Contact phone number, if provided", example = "+1 555-0100")
        String phone,

        @Schema(description = "When the profile was created")
        Instant createdAt,

        @Schema(description = "When the profile was last updated")
        Instant updatedAt
) {
}
