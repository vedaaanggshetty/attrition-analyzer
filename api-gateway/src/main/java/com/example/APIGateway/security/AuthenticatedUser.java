package com.example.APIGateway.security;

import java.util.UUID;

/**
 * Authenticated principal populated by {@link JwtAuthenticationFilter} from
 * a validated JWT's claims (issued by Authentication Service). Carries the
 * userId (JWT {@code sub}) alongside the email and role claims. The Gateway
 * only uses this to decide access - it never forwards or duplicates profile
 * data.
 */
public record AuthenticatedUser(UUID userId, String email, String role) {
}
