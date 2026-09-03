package com.example.UserProfileService.security;

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

/**
 * Verifies the security filter chain itself (public vs. protected routes),
 * mirroring authentication-service's test of the same name. Runs against
 * the real database, seeding its own profile row inside a transaction that
 * Spring's test framework rolls back automatically afterward.
 *
 * Tokens are built manually with the same secret configured in this
 * module's test {@code application.properties} - this service never issues
 * its own tokens (only Authentication does), so there is no {@code
 * generateToken} method to call here.
 */
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

    private UUID seededUserId;

    @BeforeEach
    void seedProfile() {
        seededUserId = UUID.randomUUID();
        userProfileRepository.save(new UserProfile(seededUserId, "HR User", "hr-chain@example.com", null));
    }

    @Test
    void register_isPubliclyAccessibleWithoutToken() throws Exception {
        // A deliberately invalid request (blank fullName) is used so this
        // test never needs a real Authentication Service/Eureka to be
        // running - validation fails before the Feign call would happen.
        // A 400 (not 401/403) proves the endpoint is reachable without a JWT.
        String body = objectMapper.writeValueAsString(new com.example.UserProfileService.dto.RegisterUserRequest(
                "", "someone-else@example.com", "Password123!", null));

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actuatorHealth_isPubliclyAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyProfile_withoutToken_isRejected() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyProfile_withValidToken_isAccessible() throws Exception {
        String token = buildToken(seededUserId, "hr-chain@example.com", "HR");

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String buildToken(UUID userId, String email, String role) {
        SecretKey signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(signingKey)
                .compact();
    }
}
