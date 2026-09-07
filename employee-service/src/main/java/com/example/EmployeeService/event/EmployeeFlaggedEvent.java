package com.example.EmployeeService.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to the {@code employee.flagged} Kafka topic when an HR user
 * flags an employee for Notification Service to act on. {@code eventId} is
 * the idempotency key the consumer uses to reject duplicate deliveries.
 */
@Schema(description = "Echoed back to the caller of POST /employees/{id}/flag - the same event published to Kafka")
public record EmployeeFlaggedEvent(
		UUID eventId,
		String employeeId,
		String employeeName,
		String department,
		String comment,
		String hrUserEmail,
		String hrUserName,
		Instant flaggedAt) {
}
