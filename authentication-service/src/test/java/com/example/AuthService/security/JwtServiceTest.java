package com.example.AuthService.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-value-not-for-production-use-1234567890";

    private final JwtService jwtService = new JwtService(SECRET, 3600000L);

    @Test
    void parseClaims_withValidToken_returnsEmailAndRole() {
        String token = jwtService.generateToken("hr@example.com", "HR");

        Optional<Claims> claims = jwtService.parseClaims(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("hr@example.com");
        assertThat(claims.get().get(JwtService.ROLE_CLAIM)).isEqualTo("HR");
    }

    @Test
    void parseClaims_withExpiredToken_returnsEmpty() {
        JwtService shortLivedService = new JwtService(SECRET, -1000L);
        String expiredToken = shortLivedService.generateToken("hr@example.com", "HR");

        Optional<Claims> claims = jwtService.parseClaims(expiredToken);

        assertThat(claims).isEmpty();
    }

    @Test
    void parseClaims_withTamperedSignature_returnsEmpty() {
        SecretKey differentKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-value-1234567890-abcdef".getBytes(StandardCharsets.UTF_8));
        String tokenSignedWithDifferentKey = Jwts.builder()
                .subject("hr@example.com")
                .claim(JwtService.ROLE_CLAIM, "HR")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(differentKey)
                .compact();

        Optional<Claims> claims = jwtService.parseClaims(tokenSignedWithDifferentKey);

        assertThat(claims).isEmpty();
    }

    @Test
    void parseClaims_withMalformedToken_returnsEmpty() {
        Optional<Claims> claims = jwtService.parseClaims("not-a-valid-jwt-token");

        assertThat(claims).isEmpty();
    }
}
