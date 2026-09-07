package com.example.UserProfileService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Public registration request body ({@code POST /users/register}).
 *
 * {@code password} is forwarded to Authentication Service via Feign and is
 * never persisted in User Profile's own database.
 */
@Schema(description = "New HR user sign-up details")
public record RegisterUserRequest(

        @Schema(description = "Full display name", example = "Jordan Lee")
        @NotBlank(message = "Full name is required")
        String fullName,

        @Schema(description = "Email to log in with - must not already be registered", example = "new-hr@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Password - hashed by Authentication Service, never stored here", example = "Password123!")
        @NotBlank(message = "Password is required")
        String password,

        @Schema(description = "Optional contact phone number", example = "+1 555-0100")
        String phone
) {
}
