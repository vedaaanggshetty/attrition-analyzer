package com.example.UserProfileService.controller;

import com.example.UserProfileService.dto.InternalProfileResponse;
import com.example.UserProfileService.dto.ProfileResponse;
import com.example.UserProfileService.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * INTERNAL service-to-service contract only.
 *
 * {@code GET /internal/profiles/{userId}} is called by Notification Service
 * (via Feign) to resolve a display name for a userId when a notification has
 * no name captured directly (see NotificationService.resolveSenderName).
 * Same trust model as authentication-service's {@code /internal/credentials}
 * - not reachable from the frontend/Gateway, no JWT required.
 */
@Tag(name = "Internal Profiles", description = "Service-to-service only - called by Notification Service via Feign, never by the frontend")
@RestController
@RequestMapping("/internal/profiles")
public class InternalProfileController {

    private final ProfileService profileService;

    public InternalProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(
            summary = "Get a user's display name by id",
            description = "Called internally by Notification Service to resolve the HR display name for a "
                    + "userId - not reachable from the frontend/Gateway. No JWT is required since this is a "
                    + "trusted internal call."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InternalProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "No profile exists for this userId", content = @Content)
    })
    @GetMapping("/{userId}")
    public ResponseEntity<InternalProfileResponse> getProfile(
            @Parameter(description = "The user id to look up", required = true) @PathVariable UUID userId) {
        ProfileResponse profile = profileService.getProfile(userId);
        return ResponseEntity.ok(new InternalProfileResponse(profile.userId(), profile.fullName()));
    }
}
