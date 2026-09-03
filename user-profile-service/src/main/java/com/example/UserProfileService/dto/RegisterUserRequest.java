package com.example.UserProfileService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Public registration request body ({@code POST /users/register}).
 *
 * {@code password} is forwarded to Authentication Service via Feign and is
 * never persisted in User Profile's own database.
 */
public record RegisterUserRequest(

        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password,

        String phone
) {
}
