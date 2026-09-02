package com.example.AuthService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /auth/reset-password/request}. Always
 * produces a generic 200 response regardless of whether the email is
 * registered, to avoid leaking which emails have accounts.
 */
public record PasswordResetRequestRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
) {
}
