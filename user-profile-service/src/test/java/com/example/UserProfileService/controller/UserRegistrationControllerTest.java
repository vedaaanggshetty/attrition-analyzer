package com.example.UserProfileService.controller;

import com.example.UserProfileService.dto.RegisterUserRequest;
import com.example.UserProfileService.dto.RegisterUserResponse;
import com.example.UserProfileService.exception.AuthenticationServiceException;
import com.example.UserProfileService.exception.DuplicateEmailException;
import com.example.UserProfileService.security.JwtAuthenticationFilter;
import com.example.UserProfileService.security.JwtService;
import com.example.UserProfileService.security.SecurityConfig;
import com.example.UserProfileService.service.UserRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserRegistrationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class UserRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @Test
    void register_withValidRequest_returns201AndProfile() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "Jane HR", "jane@example.com", "Password123!", "555-1234");
        UUID userId = UUID.randomUUID();

        when(userRegistrationService.register(any(RegisterUserRequest.class)))
                .thenReturn(new RegisterUserResponse(userId, "Jane HR", "jane@example.com", "555-1234", Instant.now()));

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "Jane HR", "jane@example.com", "Password123!", null);

        when(userRegistrationService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateEmailException());

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void register_whenAuthenticationServiceUnavailable_returns503() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "Jane HR", "jane@example.com", "Password123!", null);

        when(userRegistrationService.register(any(RegisterUserRequest.class)))
                .thenThrow(new AuthenticationServiceException("boom", new RuntimeException("boom")));

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void register_withBlankFullName_returns400() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "", "jane@example.com", "Password123!", null);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
