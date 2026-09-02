package com.example.AuthService.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /auth/reset-password/confirm}.
 */
public record PasswordResetConfirmRequest(

        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "New password is required")
        String newPassword
) {
}
