package com.example.UserProfileService.service;

import com.example.UserProfileService.client.AuthenticationClient;
import com.example.UserProfileService.dto.AuthCredentialRequest;
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
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void register_withNewEmail_callsAuthenticationAndPersistsProfileWithSameUserId() {
        RegisterUserRequest request = new RegisterUserRequest(
                "Jane HR", "jane@example.com", "Password123!", "555-1234");
        UUID userId = UUID.randomUUID();

        when(userProfileRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(authenticationClient.registerCredential(new AuthCredentialRequest("jane@example.com", "Password123!")))
                .thenReturn(new AuthCredentialResponse(userId));
        when(userProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterUserResponse response = userRegistrationService.register(request);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());

        UserProfile saved = captor.getValue();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getFullName()).isEqualTo("Jane HR");
        assertThat(saved.getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getPhone()).isEqualTo("555-1234");
    }

    @Test
    void register_withEmailAlreadyInProfileDb_throwsDuplicateEmailWithoutCallingAuthentication() {
        RegisterUserRequest request = new RegisterUserRequest(
                "Jane HR", "existing@example.com", "Password123!", null);

        when(userProfileRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userRegistrationService.register(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("Email is already registered");

        verify(authenticationClient, never()).registerCredential(any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void register_whenAuthenticationReportsDuplicate_throwsDuplicateEmail() {
        RegisterUserRequest request = new RegisterUserRequest(
                "Jane HR", "race@example.com", "Password123!", null);

        when(userProfileRepository.existsByEmail("race@example.com")).thenReturn(false);
        when(authenticationClient.registerCredential(any())).thenThrow(conflictFeignException());

        assertThatThrownBy(() -> userRegistrationService.register(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void register_whenAuthenticationServiceUnavailable_throwsAuthenticationServiceException() {
        RegisterUserRequest request = new RegisterUserRequest(
                "Jane HR", "down@example.com", "Password123!", null);

        when(userProfileRepository.existsByEmail("down@example.com")).thenReturn(false);
        when(authenticationClient.registerCredential(any())).thenThrow(serverErrorFeignException());

        assertThatThrownBy(() -> userRegistrationService.register(request))
                .isInstanceOf(AuthenticationServiceException.class);

        verify(userProfileRepository, never()).save(any());
    }

    private FeignException.Conflict conflictFeignException() {
        Request request = Request.create(Request.HttpMethod.POST, "/internal/credentials",
                java.util.Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.Conflict("conflict", request, null, null);
    }

    private FeignException serverErrorFeignException() {
        Request request = Request.create(Request.HttpMethod.POST, "/internal/credentials",
                java.util.Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.InternalServerError("server error", request, null, null);
    }
}
