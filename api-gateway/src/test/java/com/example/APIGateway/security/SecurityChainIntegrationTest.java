package com.example.APIGateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Proves which routes are public vs. protected at the Gateway. Downstream
// services aren't running here, so public routes are only checked as "not
// 401" (proof the Gateway let the request through without a token), and
// protected-route checks use an unmapped path - a 404 after a valid token
// proves the request passed the auth layer even with no matching route.
@SpringBootTest
@AutoConfigureMockMvc
class SecurityChainIntegrationTest {

    private static final String TEST_SECRET = "test-only-secret-value-not-for-production-use-1234567890";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowActuatorHealthWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void shouldAllowLoginWithoutToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"hr@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void shouldAllowRegisterWithoutToken() throws Exception {
        mockMvc.perform(post("/users/register")
                        .contentType("application/json")
                        .content("{\"fullName\":\"HR\",\"email\":\"hr@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void shouldRejectUnknownRouteWithoutToken() throws Exception {
        mockMvc.perform(get("/some/protected/path")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnknownRouteWithInvalidToken() throws Exception {
        mockMvc.perform(get("/some/protected/path").header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnknownRouteWithExpiredToken() throws Exception {
        String expiredToken = tokenExpiringAt(System.currentTimeMillis() - 1000L);

        mockMvc.perform(get("/some/protected/path").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPassAuthLayerWithValidTokenOnUnknownRoute() throws Exception {
        String token = tokenExpiringAt(System.currentTimeMillis() + 3600000L);

        // 404 (not 401) proves the request was authenticated - the path
        // itself simply doesn't map to any configured route.
        mockMvc.perform(get("/some/protected/path").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String tokenExpiringAt(long expirationEpochMillis) {
        SecretKey signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "hr@example.com")
                .claim("role", "HR")
                .expiration(new Date(expirationEpochMillis))
                .signWith(signingKey)
                .compact();
    }
}
