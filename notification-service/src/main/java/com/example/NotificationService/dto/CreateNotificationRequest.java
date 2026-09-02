package com.example.NotificationService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /notifications} (US-17: create a notification
 * about an employee; US-18: the comment is a field on this same request).
 * Employee details are supplied by the caller (already known from viewing the
 * employee, per Employee Service) rather than looked up live here.
 */
public record CreateNotificationRequest(

        @NotBlank(message = "Employee id is required")
        String employeeId,

        @NotBlank(message = "Employee name is required")
        String employeeName,

        @NotBlank(message = "Department is required")
        String department,

        @NotBlank(message = "Comment is required")
        @Size(max = 1000, message = "Comment must be at most 1000 characters")
        String comment
) {
}
