package com.example.AuthService.security;

import java.util.UUID;

/**
 * Authenticated principal populated by {@link JwtAuthenticationFilter} from a
 * validated JWT's claims. Carries the userId (JWT {@code sub}) alongside the
 * email and role claims so downstream code never has to re-parse the token.
 */
public record AuthenticatedUser(UUID userId, String email, String role) {
}
