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

// Proves which routes are actually public vs. protected, end to end through
// the real security filter chain - not just unit-testing the filter in isolation.
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

    private UUID userId;

    @BeforeEach
    void seedCredential() {
        userId = UUID.randomUUID();
        credentialRepository.save(new Credential(userId, "hr@example.com", passwordEncoder.encode("Password123!"), Role.HR));
    }

    @Test
    void shouldAllowLoginWithoutToken() throws Exception {
        LoginRequest request = new LoginRequest("hr@example.com", "Password123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowActuatorHealthWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void shouldRejectLogoutWithoutToken() throws Exception {
        mockMvc.perform(post("/auth/logout")).andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowLogoutWithValidToken() throws Exception {
        String token = jwtService.generateToken(userId, "hr@example.com", "HR");

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnknownRouteWithoutToken() throws Exception {
        mockMvc.perform(get("/some/protected/path")).andExpect(status().isForbidden());
    }
}
