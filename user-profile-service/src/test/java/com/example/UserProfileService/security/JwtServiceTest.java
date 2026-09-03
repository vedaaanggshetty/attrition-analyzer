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

/**
 * Unit test for {@link JwtService} (parse/verify-only in this service - see
 * class Javadoc). Tokens are built manually with the same secret since this
 * service never issues its own tokens.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-value-not-for-production-use-1234567890";

    private final JwtService jwtService = new JwtService(SECRET);
    private final SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Test
    void parseClaims_withValidToken_returnsUserIdEmailAndRole() {
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, "hr@example.com", "HR", System.currentTimeMillis() + 3600000L);

        Optional<Claims> claims = jwtService.parseClaims(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get().get(JwtService.EMAIL_CLAIM)).isEqualTo("hr@example.com");
        assertThat(claims.get().get(JwtService.ROLE_CLAIM)).isEqualTo("HR");
    }

    @Test
    void parseClaims_withExpiredToken_returnsEmpty() {
        String expiredToken = buildToken(UUID.randomUUID(), "hr@example.com", "HR", System.currentTimeMillis() - 1000L);

        Optional<Claims> claims = jwtService.parseClaims(expiredToken);

        assertThat(claims).isEmpty();
    }

    @Test
    void parseClaims_withTamperedSignature_returnsEmpty() {
        SecretKey differentKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-value-1234567890-abcdef".getBytes(StandardCharsets.UTF_8));
        String tokenSignedWithDifferentKey = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim(JwtService.EMAIL_CLAIM, "hr@example.com")
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

    private String buildToken(UUID userId, String email, String role, long expirationEpochMillis) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim(JwtService.EMAIL_CLAIM, email)
                .claim(JwtService.ROLE_CLAIM, role)
                .issuedAt(new Date())
                .expiration(new Date(expirationEpochMillis))
                .signWith(signingKey)
                .compact();
    }
}
