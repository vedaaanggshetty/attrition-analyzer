package com.example.UserProfileService.dto;

import java.util.UUID;

/**
 * Response body returned by Authentication Service's internal contract
 * ({@code POST /internal/credentials}). Only the generated {@code userId}
 * is needed - it becomes this profile's primary key.
 */
public record AuthCredentialResponse(UUID userId) {
}
