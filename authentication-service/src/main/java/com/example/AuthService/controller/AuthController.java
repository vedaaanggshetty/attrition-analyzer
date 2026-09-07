package com.example.AuthService.controller;

import com.example.AuthService.dto.ErrorResponse;
import com.example.AuthService.dto.LoginRequest;
import com.example.AuthService.dto.LoginResponse;
import com.example.AuthService.dto.MessageResponse;
import com.example.AuthService.service.AuthService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Login and session endpoints for HR users")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Log in with email and password",
            description = "Verifies credentials and issues a signed JWT. Public - no token required to call this."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login succeeded",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Email or password is incorrect",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request body failed validation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Email and password to authenticate", required = true)
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * JWT is stateless: there is no server-side session to invalidate.
     * The client is responsible for discarding the token; this endpoint
     * exists to satisfy US-03 and simply confirms the caller held a valid
     * JWT (enforced by the security filter chain) at call time. Session
     * expiry itself is handled entirely by the JWT {@code exp} claim (US-05).
     */
    @Operation(
            summary = "Log out the current session",
            description = "JWT is stateless, so there is nothing to invalidate server-side - this only confirms "
                    + "the caller held a valid token at call time. The frontend discards the token locally.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout acknowledged",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token", content = @Content)
    })
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }
}
