package com.example.NotificationService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "One notification, visible to every HR user regardless of who created it")
public record NotificationDto(

        @Schema(description = "Notification id")
        Long id,

        @Schema(description = "The flagged employee's id", example = "3012-1A41")
        String employeeId,

        @Schema(description = "The flagged employee's name", example = "Leonelle Simco")
        String employeeName,

        @Schema(description = "The flagged employee's department", example = "Sales")
        String department,

        @Schema(description = "Why the employee was flagged", example = "Flight risk, discuss retention")
        String comment,

        @Schema(description = "When this notification was created")
        Instant createdAt,

        @Schema(description = "Email of the HR user who sent this", example = "hr@example.com")
        String senderEmail,

        @Schema(description = "Display name of the sender - falls back to their email if no name was captured", example = "Jordan Lee")
        String senderName,

        @Schema(description = "Whether any HR user has reviewed this yet - a shared flag, not per-viewer")
        boolean read) {
}
