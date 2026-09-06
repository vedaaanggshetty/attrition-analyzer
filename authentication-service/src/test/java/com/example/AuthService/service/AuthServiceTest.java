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
import com.example.AuthService.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private CredentialRepository credentialRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        credentialRepository = mock(CredentialRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(credentialRepository, passwordEncoder, jwtService);
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        UUID userId = UUID.randomUUID();
        Credential credential = new Credential(userId, "hr@example.com", "hashed-password", Role.HR);
        LoginRequest request = new LoginRequest("hr@example.com", "Password123!");

        when(credentialRepository.findByEmail("hr@example.com")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("Password123!", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(userId, "hr@example.com", "HR")).thenReturn("signed-jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("signed-jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMs()).isEqualTo(3600000L);
    }

    @Test
    void login_withUnknownEmail_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("unknown@example.com", "whatever");

        when(credentialRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentials() {
        Credential credential = new Credential(UUID.randomUUID(), "hr@example.com", "hashed-password", Role.HR);
        LoginRequest request = new LoginRequest("hr@example.com", "wrong-password");

        when(credentialRepository.findByEmail("hr@example.com")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void registerCredential_withNewEmail_createsAndPersistsCredential() {
        RegisterCredentialRequest request = new RegisterCredentialRequest("new-hr@example.com", "Password123!");

        when(credentialRepository.existsByEmail("new-hr@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");

        RegisterCredentialResponse response = authService.registerCredential(request);

        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(captor.capture());

        Credential saved = captor.getValue();
        assertThat(response.userId()).isEqualTo(saved.getUserId());
        assertThat(saved.getEmail()).isEqualTo("new-hr@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(saved.getRole()).isEqualTo(Role.HR);
    }

    @Test
    void registerCredential_withDuplicateEmail_throwsDuplicateEmail() {
        RegisterCredentialRequest request = new RegisterCredentialRequest("existing@example.com", "Password123!");

        when(credentialRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerCredential(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("Email is already registered");

        verify(credentialRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerCredential_storesHashedPassword_neverPlaintext() {
        RegisterCredentialRequest request = new RegisterCredentialRequest("hashed-check@example.com", "Password123!");

        when(credentialRepository.existsByEmail("hashed-check@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");

        authService.registerCredential(request);

        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(captor.capture());

        assertThat(captor.getValue().getPasswordHash())
                .isEqualTo("hashed-password")
                .isNotEqualTo("Password123!");
    }
}
