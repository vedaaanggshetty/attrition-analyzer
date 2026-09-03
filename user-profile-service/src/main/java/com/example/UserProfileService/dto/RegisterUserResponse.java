package com.example.UserProfileService.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public registration response body. Intentionally exposes only profile
 * fields - never a password or password hash.
 */
public record RegisterUserResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        Instant createdAt
) {
}
