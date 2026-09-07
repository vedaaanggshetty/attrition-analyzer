package com.example.UserProfileService.controller;

import com.example.UserProfileService.dto.ErrorResponse;
import com.example.UserProfileService.dto.RegisterUserRequest;
import com.example.UserProfileService.dto.RegisterUserResponse;
import com.example.UserProfileService.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public registration endpoint (US-01). Coordinates with Authentication
 * Service internally (via Feign) but exposes only the profile-facing
 * contract to clients.
 */
@Tag(name = "Registration", description = "Public sign-up - the only way a Guest becomes an HR user")
@RestController
@RequestMapping("/users")
public class UserRegistrationController {

    private final UserRegistrationService userRegistrationService;

    public UserRegistrationController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @Operation(
            summary = "Register a new HR user",
            description = "Public - no token required. Creates a profile here and a credential in Authentication "
                    + "Service (via Feign), linked by a shared userId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegisterUserResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email is already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request body failed validation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Authentication Service is unavailable",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Full name, email, password, and optional phone", required = true)
            @Valid @RequestBody RegisterUserRequest request) {
        RegisterUserResponse response = userRegistrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
