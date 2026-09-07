package com.example.UserProfileService.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// This service only verifies tokens (Authentication Service issues them),
// so tokens here are built by hand rather than via a generateToken method.
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-value-not-for-production-use-1234567890";

    private final JwtService jwtService = new JwtService(SECRET);
    private final SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Test
    void shouldReadClaimsFromValidToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenExpiringAt(userId, System.currentTimeMillis() + 3600000L);

        Optional<Claims> claims = jwtService.parseClaims(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo(userId.toString());
    }

    @Test
    void shouldRejectExpiredToken() {
        String expiredToken = tokenExpiringAt(UUID.randomUUID(), System.currentTimeMillis() - 1000L);

        assertThat(jwtService.parseClaims(expiredToken)).isEmpty();
    }

    @Test
    void shouldRejectTokenSignedWithWrongSecret() {
        SecretKey wrongKey = Keys.hmacShaKeyFor("a-completely-different-secret-value-1234567890-abcdef".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .expiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(wrongKey)
                .compact();

        assertThat(jwtService.parseClaims(token)).isEmpty();
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThat(jwtService.parseClaims("not-a-valid-jwt-token")).isEmpty();
    }

    private String tokenExpiringAt(UUID userId, long expirationEpochMillis) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim(JwtService.EMAIL_CLAIM, "hr@example.com")
                .claim(JwtService.ROLE_CLAIM, "HR")
                .expiration(new Date(expirationEpochMillis))
                .signWith(signingKey)
                .compact();
    }
}
