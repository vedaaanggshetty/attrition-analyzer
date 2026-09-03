package com.example.EmployeeService.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to the {@code employee.flagged} Kafka topic when an HR user
 * flags an employee for Notification Service to act on. {@code eventId} is
 * the idempotency key the consumer uses to reject duplicate deliveries.
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
