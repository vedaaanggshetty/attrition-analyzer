package com.example.AuthService.controller;

import com.example.AuthService.dto.LoginRequest;
import com.example.AuthService.dto.LoginResponse;
import com.example.AuthService.exception.InvalidCredentialsException;
import com.example.AuthService.exception.InvalidResetTokenException;
import com.example.AuthService.security.JwtAuthenticationFilter;
import com.example.AuthService.security.JwtService;
import com.example.AuthService.security.SecurityConfig;
import com.example.AuthService.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @Test
    void login_withValidCredentials_returns200AndToken() throws Exception {
        LoginRequest request = new LoginRequest("hr@example.com", "Password123!");
        LoginResponse response = new LoginResponse("signed-jwt-token", "Bearer", 3600000L);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest("hr@example.com", "wrong-password");

        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_withBlankEmail_returns400() throws Exception {
        LoginRequest request = new LoginRequest("", "Password123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_withValidToken_returns200() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), "hr@example.com", "HR");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void logout_withoutToken_returns403() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestPasswordReset_withValidEmail_returns200() throws Exception {
        doNothing().when(authService).requestPasswordReset(any());

        mockMvc.perform(post("/auth/reset-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"hr@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void requestPasswordReset_withBlankEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/reset-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmPasswordReset_withValidRequest_returns200() throws Exception {
        doNothing().when(authService).confirmPasswordReset(any());

        mockMvc.perform(post("/auth/reset-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"some-token\",\"newPassword\":\"NewPassword123!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void confirmPasswordReset_withInvalidToken_returns400() throws Exception {
        doThrow(new InvalidResetTokenException()).when(authService).confirmPasswordReset(any());

        mockMvc.perform(post("/auth/reset-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"bad-token\",\"newPassword\":\"NewPassword123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token"));
    }

    @Test
    void confirmPasswordReset_withBlankToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/reset-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\",\"newPassword\":\"NewPassword123!\"}"))
                .andExpect(status().isBadRequest());
    }
}
