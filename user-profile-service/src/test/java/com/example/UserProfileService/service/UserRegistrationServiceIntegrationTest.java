package com.example.UserProfileService.service;

import com.example.UserProfileService.client.AuthenticationClient;
import com.example.UserProfileService.dto.AuthCredentialResponse;
import com.example.UserProfileService.dto.RegisterUserRequest;
import com.example.UserProfileService.dto.RegisterUserResponse;
import com.example.UserProfileService.entity.UserProfile;
import com.example.UserProfileService.exception.AuthenticationServiceException;
import com.example.UserProfileService.exception.DuplicateEmailException;
import com.example.UserProfileService.repository.UserProfileRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Runs against a real (in-memory H2) database. AuthenticationClient is
// mocked - a real network call to authentication-service isn't part of this
// service's own test scope.
@SpringBootTest
@Transactional
class UserRegistrationServiceIntegrationTest {

    @Autowired
    private UserRegistrationService userRegistrationService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @MockitoBean
    private AuthenticationClient authenticationClient;

    @Test
    void shouldPersistProfileWithUserIdFromAuthentication() {
        UUID userId = UUID.randomUUID();
        when(authenticationClient.registerCredential(any())).thenReturn(new AuthCredentialResponse(userId));

        RegisterUserResponse response = userRegistrationService.register(
                new RegisterUserRequest("Integration HR", "integration-profile@example.com", "Password123!", "555-0000"));

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(userProfileRepository.findById(userId)).isPresent();
    }

    @Test
    void shouldRejectEmailAlreadyPersisted() {
        userProfileRepository.save(new UserProfile(UUID.randomUUID(), "Existing HR", "existing-profile@example.com", null));

        assertThatThrownBy(() -> userRegistrationService.register(
                new RegisterUserRequest("Existing HR", "existing-profile@example.com", "Password123!", null)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void shouldNotPersistProfileWhenAuthenticationServiceFails() {
        Request feignRequest = Request.create(Request.HttpMethod.POST, "/internal/credentials", Collections.emptyMap(), null, new RequestTemplate());
        when(authenticationClient.registerCredential(any()))
                .thenThrow(new FeignException.InternalServerError("server error", feignRequest, null, null));

        assertThatThrownBy(() -> userRegistrationService.register(
                new RegisterUserRequest("Integration HR", "auth-down@example.com", "Password123!", null)))
                .isInstanceOf(AuthenticationServiceException.class);

        assertThat(userProfileRepository.existsByEmail("auth-down@example.com")).isFalse();
    }
}
