package com.example.UserProfileService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Public registration response body. Intentionally exposes only profile
 * fields - never a password or password hash.
 */
@Schema(description = "The newly created profile")
public record RegisterUserResponse(

        @Schema(description = "Identity shared with Authentication Service")
        UUID userId,

        @Schema(description = "Display name", example = "Jordan Lee")
        String fullName,

        @Schema(description = "Registered email address", example = "new-hr@example.com")
        String email,

        @Schema(description = "Contact phone number, if provided", example = "+1 555-0100")
        String phone,

        @Schema(description = "When the profile was created")
        Instant createdAt
) {
}
