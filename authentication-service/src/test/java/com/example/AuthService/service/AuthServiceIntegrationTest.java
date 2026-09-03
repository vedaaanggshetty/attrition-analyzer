package com.example.AuthService.service;

import com.example.AuthService.dto.LoginRequest;
import com.example.AuthService.dto.LoginResponse;
import com.example.AuthService.dto.PasswordResetConfirmRequest;
import com.example.AuthService.dto.PasswordResetRequestRequest;
import com.example.AuthService.dto.RegisterCredentialRequest;
import com.example.AuthService.dto.RegisterCredentialResponse;
import com.example.AuthService.entity.Credential;
import com.example.AuthService.entity.PasswordResetToken;
import com.example.AuthService.entity.Role;
import com.example.AuthService.exception.DuplicateEmailException;
import com.example.AuthService.exception.InvalidCredentialsException;
import com.example.AuthService.exception.InvalidResetTokenException;
import com.example.AuthService.repository.CredentialRepository;
import com.example.AuthService.repository.PasswordResetTokenRepository;
import com.example.AuthService.security.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test proving {@link AuthService} authenticates against the
 * real {@code credentials} table in MySQL (not a mock), covering the three
 * scenarios required for the DB-backed login migration: valid credentials,
 * unknown email, and wrong password.
 *
 * Each test seeds its own row inside a transaction that Spring's test
 * framework rolls back afterward, so no manual cleanup is needed and the
 * table is left exactly as it was found.
 */
@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

    private static final String EMAIL = "integration-test-hr@example.com";
    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    private AuthService authService;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Credential seededCredential;

    @BeforeEach
    void seedCredential() {
        seededCredential = credentialRepository.save(new Credential(
                UUID.randomUUID(),
                EMAIL,
                passwordEncoder.encode(RAW_PASSWORD),
                Role.HR));
    }

    @Test
    void login_withValidCredentials_returnsSignedToken() {
        LoginResponse response = authService.login(new LoginRequest(EMAIL, RAW_PASSWORD));

        assertThat(response.token()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_withUnknownEmail_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("does-not-exist@example.com", RAW_PASSWORD);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest(EMAIL, "wrong-password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void registerCredential_withNewEmail_persistsCredentialAndReturnsMatchingUserId() {
        RegisterCredentialRequest request = new RegisterCredentialRequest(
                "integration-test-register@example.com", "Password123!");

        RegisterCredentialResponse response = authService.registerCredential(request);

        Optional<Credential> persisted = credentialRepository.findById(response.userId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getUserId()).isEqualTo(response.userId());
        assertThat(persisted.get().getEmail()).isEqualTo("integration-test-register@example.com");
        assertThat(persisted.get().getRole()).isEqualTo(Role.HR);
        assertThat(persisted.get().getPasswordHash()).isNotEqualTo("Password123!");
    }

    @Test
    void registerCredential_withDuplicateEmail_throwsDuplicateEmail() {
        RegisterCredentialRequest request = new RegisterCredentialRequest(EMAIL, "AnotherPassword123!");

        assertThatThrownBy(() -> authService.registerCredential(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void requestPasswordReset_withKnownEmail_persistsResetTokenForThatUser() {
        authService.requestPasswordReset(new PasswordResetRequestRequest(EMAIL));

        boolean hasTokenForUser = passwordResetTokenRepository.findAll().stream()
                .anyMatch(t -> t.getUserId().equals(seededCredential.getUserId()));
        assertThat(hasTokenForUser).isTrue();
    }

    @Test
    void requestPasswordReset_withUnknownEmail_doesNotPersistToken() {
        long before = passwordResetTokenRepository.count();

        authService.requestPasswordReset(new PasswordResetRequestRequest("no-such-user@example.com"));

        assertThat(passwordResetTokenRepository.count()).isEqualTo(before);
    }

    @Test
    void confirmPasswordReset_withValidToken_updatesPasswordAndAllowsLoginWithNewPassword() {
        String rawToken = "integration-valid-token";
        passwordResetTokenRepository.save(new PasswordResetToken(
                UUID.randomUUID(), seededCredential.getUserId(), TokenHashUtil.sha256Hex(rawToken),
                Instant.now().plus(15, ChronoUnit.MINUTES)));

        authService.confirmPasswordReset(new PasswordResetConfirmRequest(rawToken, "NewPassword123!"));

        LoginResponse response = authService.login(new LoginRequest(EMAIL, "NewPassword123!"));
        assertThat(response.token()).isNotBlank();

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void confirmPasswordReset_withExpiredToken_throwsInvalidResetToken() {
        String rawToken = "integration-expired-token";
        passwordResetTokenRepository.save(new PasswordResetToken(
                UUID.randomUUID(), seededCredential.getUserId(), TokenHashUtil.sha256Hex(rawToken),
                Instant.now().minus(1, ChronoUnit.MINUTES)));

        assertThatThrownBy(() -> authService.confirmPasswordReset(
                new PasswordResetConfirmRequest(rawToken, "NewPassword123!")))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void confirmPasswordReset_withAlreadyUsedToken_throwsInvalidResetToken() {
        String rawToken = "integration-used-token";
        PasswordResetToken resetToken = new PasswordResetToken(
                UUID.randomUUID(), seededCredential.getUserId(), TokenHashUtil.sha256Hex(rawToken),
                Instant.now().plus(15, ChronoUnit.MINUTES));
        resetToken.markUsed();
        passwordResetTokenRepository.save(resetToken);

        assertThatThrownBy(() -> authService.confirmPasswordReset(
                new PasswordResetConfirmRequest(rawToken, "NewPassword123!")))
                .isInstanceOf(InvalidResetTokenException.class);
    }
}
