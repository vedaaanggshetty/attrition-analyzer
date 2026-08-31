package com.example.AuthService.security;

import com.example.AuthService.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the security filter chain itself (public vs. protected routes),
 * not any specific business endpoint. No new production endpoint is
 * introduced for this test - an arbitrary non-existent path is used to
 * confirm that "authenticated()" is enforced before dispatch.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityChainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

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
    void unknownProtectedPath_withoutToken_isRejected() throws Exception {
        mockMvc.perform(get("/some/protected/path"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownProtectedPath_withValidToken_passesAuthenticationLayer() throws Exception {
        String token = jwtService.generateToken("hr@example.com", "HR");

        // 404 (not 401/403) proves the request was authenticated and reached
        // the dispatcher - the path itself simply doesn't map to a controller.
        mockMvc.perform(get("/some/protected/path")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
