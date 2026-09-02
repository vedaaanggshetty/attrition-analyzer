package com.example.NotificationService.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.example.NotificationService.exception.UnauthenticatedException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-value-not-for-production-use-1234567890";

    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    void extractEmailReturnsSubjectFromValidToken() {
        String token = Jwts.builder()
                .subject("hr@example.com")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.extractEmail(token)).isEqualTo("hr@example.com");
    }

    @Test
    void extractEmailThrowsForMalformedToken() {
        assertThatThrownBy(() -> jwtService.extractEmail("not-a-real-token"))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void extractEmailThrowsForTokenSignedWithDifferentKey() {
        String token = Jwts.builder()
                .subject("hr@example.com")
                .signWith(Keys.hmacShaKeyFor("a-completely-different-secret-value-1234567890-abcdef".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.extractEmail(token))
                .isInstanceOf(UnauthenticatedException.class);
    }
}
