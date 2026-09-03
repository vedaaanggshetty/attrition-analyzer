package com.example.NotificationService.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors employee-service's EmployeeFlaggedEvent. Kept as a separate copy
 * (not a shared library) - the two services own their own contracts, same
 * pattern as every other cross-service DTO in this project.
 */
public record EmployeeFlaggedEvent(
		UUID eventId,
		String employeeId,
		String employeeName,
		String department,
		String comment,
		String hrUserEmail,
		Instant flaggedAt) {
}
