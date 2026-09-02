package com.example.APIGateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the Gateway's security filter chain itself (public vs. protected
 * routes) - not any downstream service's business logic. Downstream
 * services/Eureka are not running in this test, so:
 * <ul>
 *   <li>public routes that proxy to a real service (login/register/reset)
 *       are only asserted as "not 401", proving the Gateway let them through
 *       without a token - what the (unavailable) downstream returns is out
 *       of scope here;</li>
 *   <li>protected-route checks use an unmapped path (mirrors
 *       authentication-service/user-profile-service's SecurityChainIntegrationTest
 *       pattern) so a 404 after a valid token proves the request passed the
 *       authentication layer, without depending on any live downstream route.</li>
 * </ul>
 * Tokens are built manually with the same secret configured in this
 * module's test application.properties - the Gateway never issues its own
 * tokens (only Authentication does).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityChainIntegrationTest {

    private static final String TEST_SECRET = "test-only-secret-value-not-for-production-use-1234567890";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealth_isPubliclyAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void login_isPubliclyAccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"hr@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void usersRegister_isPubliclyAccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/users/register")
                        .contentType("application/json")
                        .content("{\"fullName\":\"HR\",\"email\":\"hr@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void resetPasswordRequest_isPubliclyAccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/auth/reset-password/request")
                        .contentType("application/json")
                        .content("{\"email\":\"hr@example.com\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void unknownProtectedPath_withoutToken_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/some/protected/path"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownProtectedPath_withInvalidToken_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/some/protected/path")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownProtectedPath_withExpiredToken_isRejectedWith401() throws Exception {
        String expiredToken = buildToken(UUID.randomUUID(), "hr@example.com", "HR",
                System.currentTimeMillis() - 1000L);

        mockMvc.perform(get("/some/protected/path")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownProtectedPath_withValidToken_passesAuthenticationLayer() throws Exception {
        String token = buildToken(UUID.randomUUID(), "hr@example.com", "HR",
                System.currentTimeMillis() + 3600000L);

        // 404 (not 401) proves the request was authenticated and reached
        // routing - the path itself simply doesn't map to any configured route.
        mockMvc.perform(get("/some/protected/path")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String buildToken(UUID userId, String email, String role, long expirationEpochMillis) {
        SecretKey signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(expirationEpochMillis))
                .signWith(signingKey)
                .compact();
    }
}
