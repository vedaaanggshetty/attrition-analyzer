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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller test for the authenticated "my profile" endpoints. Valid JWTs
 * are built manually with the same secret configured in this module's test
 * {@code application.properties} - this service never issues its own
 * tokens, so there is no {@code generateToken} method to call.
 */
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
    void getMyProfile_withValidToken_returns200AndOwnProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, "hr@example.com", "HR");

        when(profileService.getProfile(userId)).thenReturn(new ProfileResponse(
                userId, "Jane HR", "hr@example.com", "555-1234", Instant.now(), Instant.now()));

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("hr@example.com"));
    }

    @Test
    void getMyProfile_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyProfile_whenProfileMissing_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, "hr@example.com", "HR");

        when(profileService.getProfile(userId)).thenThrow(new ProfileNotFoundException());

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMyProfile_withValidToken_returns200AndUpdatedProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, "hr@example.com", "HR");
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Updated", "555-9999");

        when(profileService.updateProfile(eq(userId), any(UpdateProfileRequest.class))).thenReturn(new ProfileResponse(
                userId, "Jane Updated", "hr@example.com", "555-9999", Instant.now(), Instant.now()));

        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Updated"))
                .andExpect(jsonPath("$.phone").value("555-9999"));
    }

    @Test
    void updateMyProfile_withoutToken_returns403() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Updated", "555-9999");

        mockMvc.perform(put("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyProfile_withBlankFullName_returns400() throws Exception {
        String token = buildToken(UUID.randomUUID(), "hr@example.com", "HR");
        UpdateProfileRequest request = new UpdateProfileRequest("", "555-9999");

        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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
