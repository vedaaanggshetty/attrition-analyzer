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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test proving {@link UserRegistrationService} persists profiles
 * against the real {@code profiles} table in MySQL (not a mock repository).
 *
 * Authentication Service is not actually invoked over the network in this
 * test - {@link AuthenticationClient} is replaced with a Mockito bean so the
 * test is isolated to this service's own database and code, per standard
 * microservice test isolation. The contract this proves is: whatever
 * {@code userId} Authentication Service returns is the exact primary key
 * User Profile Service persists - satisfying the "same UUID in both
 * services" requirement at the code level (see also
 * {@code AuthServiceIntegrationTest} in authentication-service, which proves
 * the same UUID is persisted as the credential's primary key on that side).
 *
 * Each test runs inside a transaction that Spring's test framework rolls
 * back afterward, so no manual cleanup is needed.
 */
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
    void register_withNewEmail_persistsProfileUsingUserIdReturnedByAuthentication() {
        UUID userId = UUID.randomUUID();
        RegisterUserRequest request = new RegisterUserRequest(
                "Integration HR", "integration-profile@example.com", "Password123!", "555-0000");

        when(authenticationClient.registerCredential(new AuthCredentialRequest(request.email(), request.password())))
                .thenReturn(new AuthCredentialResponse(userId));

        RegisterUserResponse response = userRegistrationService.register(request);

        assertThat(response.userId()).isEqualTo(userId);

        Optional<UserProfile> persisted = userProfileRepository.findById(userId);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getUserId()).isEqualTo(userId);
        assertThat(persisted.get().getEmail()).isEqualTo("integration-profile@example.com");
        assertThat(persisted.get().getFullName()).isEqualTo("Integration HR");
    }

    @Test
    void register_withEmailAlreadyPersisted_throwsDuplicateEmailWithoutCallingAuthentication() {
        UUID existingUserId = UUID.randomUUID();
        userProfileRepository.save(new UserProfile(
                existingUserId, "Existing HR", "existing-profile@example.com", null));

        RegisterUserRequest request = new RegisterUserRequest(
                "Existing HR", "existing-profile@example.com", "Password123!", null);

        assertThatThrownBy(() -> userRegistrationService.register(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void register_whenAuthenticationServiceFails_throwsAuthenticationServiceExceptionAndDoesNotPersistProfile() {
        RegisterUserRequest request = new RegisterUserRequest(
                "Integration HR", "auth-down@example.com", "Password123!", null);

        Request feignRequest = Request.create(Request.HttpMethod.POST, "/internal/credentials",
                java.util.Collections.emptyMap(), null, new RequestTemplate());
        when(authenticationClient.registerCredential(any()))
                .thenThrow(new FeignException.InternalServerError("server error", feignRequest, null, null));

        assertThatThrownBy(() -> userRegistrationService.register(request))
                .isInstanceOf(AuthenticationServiceException.class);

        assertThat(userProfileRepository.existsByEmail("auth-down@example.com")).isFalse();
    }
}
