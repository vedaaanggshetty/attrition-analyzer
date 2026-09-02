package com.example.NotificationService.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.NotificationService.dto.CreateNotificationRequest;
import com.example.NotificationService.dto.NotificationDto;
import com.example.NotificationService.exception.UnauthenticatedException;
import com.example.NotificationService.security.JwtService;
import com.example.NotificationService.service.NotificationService;

import jakarta.validation.Valid;

@RestController
public class NotificationController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final NotificationService notificationService;
    private final JwtService jwtService;

    public NotificationController(NotificationService notificationService, JwtService jwtService) {
        this.notificationService = notificationService;
        this.jwtService = jwtService;
    }

    @PostMapping("/notifications")
    public ResponseEntity<NotificationDto> createNotification(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateNotificationRequest request) {
        String email = currentUserEmail(authorization);
        NotificationDto created = notificationService.createNotification(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/notifications")
    public List<NotificationDto> getMyNotifications(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String email = currentUserEmail(authorization);
        return notificationService.getNotificationsForUser(email);
    }

    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String email = currentUserEmail(authorization);
        notificationService.deleteNotification(id, email);
        return ResponseEntity.noContent().build();
    }

    private String currentUserEmail(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new UnauthenticatedException();
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        return jwtService.extractEmail(token);
    }
}
