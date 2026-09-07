package com.example.UserProfileService.controller;

import com.example.UserProfileService.dto.ProfileResponse;
import com.example.UserProfileService.dto.UpdateProfileRequest;
import com.example.UserProfileService.exception.ProfileNotFoundException;
import com.example.UserProfileService.security.JwtAuthenticationFilter;
import com.example.UserProfileService.security.JwtService;
import com.example.UserProfileService.security.SecurityConfig;
import com.example.UserProfileService.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// This service never issues its own tokens, so tokens here are built by
// hand with the same secret as the test application.properties.
@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class ProfileControllerTest {

    private static final String TEST_SECRET = "test-only-secret-value-not-for-production-use-1234567890";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void shouldReturnOwnProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        when(profileService.getProfile(userId)).thenReturn(
                new ProfileResponse(userId, "Jane HR", "hr@example.com", "555-1234", Instant.now(), Instant.now()));

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + tokenFor(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("hr@example.com"));
    }

    @Test
    void shouldRejectMissingToken() throws Exception {
        mockMvc.perform(get("/users/me")).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenProfileMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        when(profileService.getProfile(userId)).thenThrow(new ProfileNotFoundException());

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + tokenFor(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        when(profileService.updateProfile(any(), any())).thenReturn(
                new ProfileResponse(userId, "Jane Updated", "hr@example.com", "555-9999", Instant.now(), Instant.now()));

        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + tokenFor(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("Jane Updated", "555-9999"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Updated"));
    }

    @Test
    void shouldRejectBlankFullNameOnUpdate() throws Exception {
        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + tokenFor(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("", "555-9999"))))
                .andExpect(status().isBadRequest());
    }

    private String tokenFor(UUID userId) {
        SecretKey signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", "hr@example.com")
                .claim("role", "HR")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(signingKey)
                .compact();
    }
}
