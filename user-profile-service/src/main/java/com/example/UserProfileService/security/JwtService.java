package com.example.UserProfileService.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Validates JWTs issued by Authentication Service. User Profile Service
 * never issues its own tokens - only Authentication does - so this class is
 * parse/verify-only, using the same shared signing secret ({@code
 * JWT_SECRET}) both services are configured with.
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    public static final String EMAIL_CLAIM = "email";
    public static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses and validates a JWT (signature + expiration). Returns an empty
     * Optional for any invalid, tampered, expired, or malformed token
     * instead of throwing, so callers (e.g. security filters) can treat
     * every failure uniformly. Never logs the token or the signing secret.
     */
    public Optional<Claims> parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
