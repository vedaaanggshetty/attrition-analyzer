package com.example.UserProfileService.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PUT /users/me}. Deliberately excludes
 * {@code userId} and {@code email} - the profile to update is always
 * identified from the caller's JWT, never from client-supplied input, and
 * email changes are out of scope for this phase (see {@code
 * UserProfile.updateProfile}).
 */
public record UpdateProfileRequest(

        @NotBlank(message = "Full name is required")
        String fullName,

        String phone
) {
}
