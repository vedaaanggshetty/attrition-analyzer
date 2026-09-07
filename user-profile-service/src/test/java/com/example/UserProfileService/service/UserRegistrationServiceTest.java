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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRegistrationServiceTest {

    private UserProfileRepository userProfileRepository;
    private AuthenticationClient authenticationClient;
    private UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        userProfileRepository = mock(UserProfileRepository.class);
        authenticationClient = mock(AuthenticationClient.class);
        userRegistrationService = new UserRegistrationService(userProfileRepository, authenticationClient);
    }

    @Test
    void shouldRegisterNewUser() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(authenticationClient.registerCredential(any())).thenReturn(new AuthCredentialResponse(userId));
        when(userProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterUserResponse response = userRegistrationService.register(
                new RegisterUserRequest("Jane HR", "jane@example.com", "Password123!", "555-1234"));

        assertThat(response.userId()).isEqualTo(userId);
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void shouldRejectEmailAlreadyInProfileDb() {
        when(userProfileRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userRegistrationService.register(
                new RegisterUserRequest("Jane HR", "existing@example.com", "Password123!", null)))
                .isInstanceOf(DuplicateEmailException.class);

        verify(authenticationClient, never()).registerCredential(any());
    }

    @Test
    void shouldRejectWhenAuthenticationReportsDuplicate() {
        when(userProfileRepository.existsByEmail("race@example.com")).thenReturn(false);
        when(authenticationClient.registerCredential(any())).thenThrow(conflictFeignException());

        assertThatThrownBy(() -> userRegistrationService.register(
                new RegisterUserRequest("Jane HR", "race@example.com", "Password123!", null)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void shouldFailClearlyWhenAuthenticationServiceIsDown() {
        when(userProfileRepository.existsByEmail("down@example.com")).thenReturn(false);
        when(authenticationClient.registerCredential(any())).thenThrow(serverErrorFeignException());

        assertThatThrownBy(() -> userRegistrationService.register(
                new RegisterUserRequest("Jane HR", "down@example.com", "Password123!", null)))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    private FeignException.Conflict conflictFeignException() {
        Request request = Request.create(Request.HttpMethod.POST, "/internal/credentials", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.Conflict("conflict", request, null, null);
    }

    private FeignException serverErrorFeignException() {
        Request request = Request.create(Request.HttpMethod.POST, "/internal/credentials", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.InternalServerError("server error", request, null, null);
    }
}
