package com.example.NotificationService.dto;

import java.util.UUID;

/**
 * Mirrors user-profile-service's InternalProfileResponse. Kept as a separate
 * copy (not a shared library) - the two services own their own contracts,
 * same pattern as every other cross-service DTO in this project (e.g.
 * EmployeeFlaggedEvent).
 */
public record InternalProfileResponse(UUID userId, String fullName) {
}
