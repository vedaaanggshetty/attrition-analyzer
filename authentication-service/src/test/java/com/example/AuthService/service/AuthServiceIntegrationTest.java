package com.example.AuthService.service;

import com.example.AuthService.dto.LoginRequest;
import com.example.AuthService.dto.LoginResponse;
import com.example.AuthService.dto.RegisterCredentialRequest;
import com.example.AuthService.dto.RegisterCredentialResponse;
import com.example.AuthService.entity.Credential;
import com.example.AuthService.entity.Role;
import com.example.AuthService.exception.DuplicateEmailException;
import com.example.AuthService.exception.InvalidCredentialsException;
import com.example.AuthService.repository.CredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs against a real (in-memory H2) database, not mocks - each test runs in
// its own transaction that gets rolled back afterward.
@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

    private static final String EMAIL = "integration-test-hr@example.com";
    private static final String PASSWORD = "Password123!";

    @Autowired
    private AuthService authService;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedCredential() {
        credentialRepository.save(new Credential(UUID.randomUUID(), EMAIL, passwordEncoder.encode(PASSWORD), Role.HR));
    }

    @Test
    void shouldLoginWithSeededCredential() {
        LoginResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.token()).isNotBlank();
    }

    @Test
    void shouldRejectUnknownEmail() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("does-not-exist@example.com", PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldRejectWrongPassword() {
        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldPersistNewCredentialOnRegister() {
        RegisterCredentialResponse response = authService.registerCredential(
                new RegisterCredentialRequest("new-user@example.com", "Password123!"));

        assertThat(credentialRepository.findById(response.userId())).isPresent();
    }

    @Test
    void shouldRejectDuplicateEmailOnRegister() {
        assertThatThrownBy(() -> authService.registerCredential(new RegisterCredentialRequest(EMAIL, "AnotherPassword123!")))
                .isInstanceOf(DuplicateEmailException.class);
    }
}
