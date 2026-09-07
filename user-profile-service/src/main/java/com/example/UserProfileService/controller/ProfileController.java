package com.example.UserProfileService.controller;

import com.example.UserProfileService.dto.ErrorResponse;
import com.example.UserProfileService.dto.ProfileResponse;
import com.example.UserProfileService.dto.UpdateProfileRequest;
import com.example.UserProfileService.security.AuthenticatedUser;
import com.example.UserProfileService.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated "my profile" endpoints (US-06, US-07). The profile acted on
 * is always the caller's own - identified from the JWT principal populated
 * by {@code JwtAuthenticationFilter} - never from a client-supplied userId.
 */
@Tag(name = "Profile", description = "View and update the caller's own HR profile")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/users")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Get the caller's own profile",
            description = "The profile returned is always the caller's own, identified from their JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token", content = @Content),
            @ApiResponse(responseCode = "404", description = "No profile exists for this user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(profileService.getProfile(principal.userId()));
    }

    @Operation(summary = "Update the caller's own profile",
            description = "Only full name and phone are editable here - email changes are out of scope for this phase.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request body failed validation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token", content = @Content),
            @ApiResponse(responseCode = "404", description = "No profile exists for this user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(@AuthenticationPrincipal AuthenticatedUser principal,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New full name and phone", required = true)
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(principal.userId(), request));
    }
}
