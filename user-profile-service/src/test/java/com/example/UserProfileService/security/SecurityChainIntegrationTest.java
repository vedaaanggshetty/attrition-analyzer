package com.example.UserProfileService.security;

import com.example.UserProfileService.dto.RegisterUserRequest;
import com.example.UserProfileService.entity.UserProfile;
import com.example.UserProfileService.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Proves which routes are public vs. protected, end to end through the real
// security filter chain. Tokens are built by hand since this service never
// issues its own (only Authentication Service does).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityChainIntegrationTest {

    private static final String TEST_SECRET = "test-only-secret-value-not-for-production-use-1234567890";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private UUID userId;

    @BeforeEach
    void seedProfile() {
        userId = UUID.randomUUID();
        userProfileRepository.save(new UserProfile(userId, "HR User", "hr-chain@example.com", null));
    }

    @Test
    void shouldAllowRegisterWithoutToken() throws Exception {
        // Blank fullName fails validation before any Feign call would happen,
        // so this test doesn't need Authentication Service running - a 400
        // (not 401/403) is enough to prove the route itself is public.
        RegisterUserRequest request = new RegisterUserRequest("", "someone-else@example.com", "Password123!", null);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowActuatorHealthWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void shouldRejectGetProfileWithoutToken() throws Exception {
        mockMvc.perform(get("/users/me")).andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowGetProfileWithValidToken() throws Exception {
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + tokenFor(userId)))
                .andExpect(status().isOk());
    }

    private String tokenFor(UUID userId) {
        SecretKey signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", "hr-chain@example.com")
                .claim("role", "HR")
                .expiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(signingKey)
                .compact();
    }
}
