package com.example.NotificationService.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.NotificationService.dto.CreateNotificationRequest;
import com.example.NotificationService.dto.ErrorResponse;
import com.example.NotificationService.dto.NotificationDto;
import com.example.NotificationService.exception.UnauthenticatedException;
import com.example.NotificationService.security.JwtService;
import com.example.NotificationService.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * This service has no Spring Security of its own - every endpoint here is
 * reachable only through the Gateway's own JWT check. It only reads the
 * JWT's email claim directly, to attribute created/reviewed notifications to
 * their caller.
 */
@Tag(name = "Notifications", description = "Notes about employees, shared across every HR user")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class NotificationController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final NotificationService notificationService;
    private final JwtService jwtService;

    public NotificationController(NotificationService notificationService, JwtService jwtService) {
        this.notificationService = notificationService;
        this.jwtService = jwtService;
    }

    @Operation(
            summary = "Create a notification directly",
            description = "Not used by the current UI (the frontend only creates notifications by flagging an "
                    + "employee, via employee-service/Kafka) but available as a direct alternative."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notification created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NotificationDto.class))),
            @ApiResponse(responseCode = "400", description = "Request body failed validation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/notifications")
    public ResponseEntity<NotificationDto> createNotification(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Employee details and comment", required = true)
            @Valid @RequestBody CreateNotificationRequest request) {
        String email = currentUserEmail(authorization);
        NotificationDto created = notificationService.createNotification(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "List every notification",
            description = "Shared across every HR user - the result is not filtered by who created each one, "
                    + "only a valid session is required to call this."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All notifications, newest first",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = NotificationDto.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/notifications")
    public List<NotificationDto> getAllNotifications(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Auth is still required (any valid HR session), but the list itself
        // is shared across every HR user rather than filtered by caller.
        currentUserEmail(authorization);
        return notificationService.getAllNotifications();
    }

    @Operation(
            summary = "Mark a notification as reviewed",
            description = "Any authenticated HR user may review a notification - review state is shared, not "
                    + "private to whoever created it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked reviewed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NotificationDto.class))),
            @ApiResponse(responseCode = "404", description = "No notification with this id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(
            @Parameter(description = "Notification id", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization) {
        currentUserEmail(authorization);
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @Operation(
            summary = "Delete a notification",
            description = "Not restricted to the notification's creator - notifications are a shared HR resource, "
                    + "so any authenticated HR user may remove one."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notification deleted", content = @Content),
            @ApiResponse(responseCode = "404", description = "No notification with this id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<Void> deleteNotification(
            @Parameter(description = "Notification id", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization) {
        currentUserEmail(authorization);
        notificationService.deleteNotification(id);
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
