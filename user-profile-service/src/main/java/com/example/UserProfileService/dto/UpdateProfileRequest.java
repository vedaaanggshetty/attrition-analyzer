package com.example.UserProfileService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PUT /users/me}. Deliberately excludes
 * {@code userId} and {@code email} - the profile to update is always
 * identified from the caller's JWT, never from client-supplied input, and
 * email changes are out of scope for this phase (see {@code
 * UserProfile.updateProfile}).
 */
@Schema(description = "Editable profile fields for the caller's own profile")
public record UpdateProfileRequest(

        @Schema(description = "Full display name", example = "Jordan A. Lee")
        @NotBlank(message = "Full name is required")
        String fullName,

        @Schema(description = "Contact phone number", example = "+1 555-0100")
        String phone
) {
}
