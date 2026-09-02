package com.example.AuthService.controller;

import com.example.AuthService.dto.LoginRequest;
import com.example.AuthService.dto.LoginResponse;
import com.example.AuthService.dto.MessageResponse;
import com.example.AuthService.dto.PasswordResetConfirmRequest;
import com.example.AuthService.dto.PasswordResetRequestRequest;
import com.example.AuthService.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * JWT is stateless: there is no server-side session to invalidate.
     * The client is responsible for discarding the token; this endpoint
     * exists to satisfy US-03 and simply confirms the caller held a valid
     * JWT (enforced by the security filter chain) at call time. Session
     * expiry itself is handled entirely by the JWT {@code exp} claim (US-05).
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @PostMapping("/reset-password/request")
    public ResponseEntity<MessageResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequestRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(new MessageResponse("If that email is registered, a reset link has been sent"));
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<MessageResponse> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully"));
    }
}
