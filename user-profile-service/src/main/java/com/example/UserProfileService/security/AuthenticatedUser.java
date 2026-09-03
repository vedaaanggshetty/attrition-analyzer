package com.example.UserProfileService.security;

import java.util.UUID;

/**
 * Authenticated principal populated by {@link JwtAuthenticationFilter} from
 * a validated JWT's claims (issued by Authentication Service). Carries the
 * userId (JWT {@code sub}) alongside the email and role claims so
 * controllers never have to re-parse the token.
 */
public record AuthenticatedUser(UUID userId, String email, String role) {
}
