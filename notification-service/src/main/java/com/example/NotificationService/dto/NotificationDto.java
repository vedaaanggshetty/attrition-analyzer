package com.example.NotificationService.dto;

import java.time.Instant;

public record NotificationDto(
        Long id,
        String employeeId,
        String employeeName,
        String department,
        String comment,
        Instant createdAt) {
}
