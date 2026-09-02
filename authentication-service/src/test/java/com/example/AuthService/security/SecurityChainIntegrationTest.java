package com.example.AuthService.security;

import com.example.AuthService.dto.LoginRequest;
import com.example.AuthService.entity.Credential;
import com.example.AuthService.entity.Role;
import com.example.AuthService.repository.CredentialRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the security filter chain itself (public vs. protected routes),
 * not any specific business endpoint. No new production endpoint is
 * introduced for this test - an arbitrary non-existent path is used to
 * confirm that "authenticated()" is enforced before dispatch.
 *
 * Runs against the real database (Credential/CredentialRepository), so each
 * test seeds its own credential row inside a transaction that Spring's test
 * framework rolls back automatically afterward - no manual cleanup needed
 * and no dependency on data left behind by other tests/runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityChainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Credential seededCredential;

    @BeforeEach
    void seedCredential() {
        seededCredential = credentialRepository.save(new Credential(
                UUID.randomUUID(),
                "hr@example.com",
                passwordEncoder.encode("Password123!"),
                Role.HR));
    }

    @Test
    void login_isPubliclyAccessibleWithoutToken() throws Exception {
        LoginRequest request = new LoginRequest("hr@example.com", "Password123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorHealth_isPubliclyAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void resetPasswordRequest_isPubliclyAccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/auth/reset-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"hr@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void logout_withoutToken_isRejected() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logout_withValidToken_isAccessible() throws Exception {
        String token = jwtService.generateToken(seededCredential.getUserId(), "hr@example.com", "HR");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void unknownProtectedPath_withoutToken_isRejected() throws Exception {
        mockMvc.perform(get("/some/protected/path"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownProtectedPath_withValidToken_passesAuthenticationLayer() throws Exception {
        String token = jwtService.generateToken(seededCredential.getUserId(), "hr@example.com", "HR");

        // 404 (not 401/403) proves the request was authenticated and reached
        // the dispatcher - the path itself simply doesn't map to a controller.
        mockMvc.perform(get("/some/protected/path")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
