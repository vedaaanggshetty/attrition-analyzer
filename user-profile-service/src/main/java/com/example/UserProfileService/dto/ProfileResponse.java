package com.example.UserProfileService.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body for {@code GET /users/me} / {@code PUT /users/me}.
 */
public record ProfileResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        Instant createdAt,
        Instant updatedAt
) {
}
