package com.example.NotificationService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /notifications} (US-17: create a notification
 * about an employee; US-18: the comment is a field on this same request).
 * Employee details are supplied by the caller (already known from viewing the
 * employee, per Employee Service) rather than looked up live here.
 */
@Schema(description = "Create a notification directly, without going through the flag-employee/Kafka flow")
public record CreateNotificationRequest(

        @Schema(description = "The employee this note is about", example = "3012-1A41")
        @NotBlank(message = "Employee id is required")
        String employeeId,

        @Schema(description = "The employee's name", example = "Leonelle Simco")
        @NotBlank(message = "Employee name is required")
        String employeeName,

        @Schema(description = "The employee's department", example = "Sales")
        @NotBlank(message = "Department is required")
        String department,

        @Schema(description = "Why this notification is being created", example = "Flight risk, discuss retention")
        @NotBlank(message = "Comment is required")
        @Size(max = 1000, message = "Comment must be at most 1000 characters")
        String comment,

        // Optional - the sending HR user's display name, if the caller has one
        // to send (the JWT itself only carries an email claim). Falls back to
        // the email in the UI when absent.
        @Schema(description = "Display name of the sending HR user, if known", example = "Jordan Lee")
        String hrUserName
) {
}
