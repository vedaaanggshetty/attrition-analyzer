package com.example.AuthService.security;

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

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-value-not-for-production-use-1234567890";

    private final JwtService jwtService = new JwtService(SECRET, 3600000L);

    @Test
    void shouldReadClaimsBackFromGeneratedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "hr@example.com", "HR");

        Optional<Claims> claims = jwtService.parseClaims(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get().get(JwtService.EMAIL_CLAIM)).isEqualTo("hr@example.com");
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtService expiredTokenService = new JwtService(SECRET, -1000L);
        String expiredToken = expiredTokenService.generateToken(UUID.randomUUID(), "hr@example.com", "HR");

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
}
