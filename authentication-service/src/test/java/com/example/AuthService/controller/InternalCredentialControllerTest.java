package com.example.AuthService.controller;

import com.example.AuthService.dto.RegisterCredentialRequest;
import com.example.AuthService.dto.RegisterCredentialResponse;
import com.example.AuthService.exception.DuplicateEmailException;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalCredentialController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class InternalCredentialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerCredential_withValidRequest_returns201AndUserId() throws Exception {
        RegisterCredentialRequest request = new RegisterCredentialRequest("hr@example.com", "Password123!");
        UUID userId = UUID.randomUUID();

        when(authService.registerCredential(any(RegisterCredentialRequest.class)))
                .thenReturn(new RegisterCredentialResponse(userId));

        mockMvc.perform(post("/internal/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void registerCredential_withDuplicateEmail_returns409() throws Exception {
        RegisterCredentialRequest request = new RegisterCredentialRequest("hr@example.com", "Password123!");

        when(authService.registerCredential(any(RegisterCredentialRequest.class)))
                .thenThrow(new DuplicateEmailException());

        mockMvc.perform(post("/internal/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void registerCredential_withBlankEmail_returns400() throws Exception {
        RegisterCredentialRequest request = new RegisterCredentialRequest("", "Password123!");

        mockMvc.perform(post("/internal/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
