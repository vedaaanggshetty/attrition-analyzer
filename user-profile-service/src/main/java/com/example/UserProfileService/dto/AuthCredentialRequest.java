package com.example.UserProfileService.dto;

/**
 * Request body sent to Authentication Service's internal contract
 * ({@code POST /internal/credentials}) via {@code AuthenticationClient}.
 * Deliberately separate from {@link RegisterUserRequest} - User Profile
 * only forwards the two fields Authentication actually needs.
 */
public record AuthCredentialRequest(String email, String password) {
}
