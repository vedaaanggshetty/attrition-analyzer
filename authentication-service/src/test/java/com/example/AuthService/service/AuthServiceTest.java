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
import static org.mockito.ArgumentMatchers.any;
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
    void shouldLoginSuccessfully() {
        UUID userId = UUID.randomUUID();
        Credential credential = new Credential(userId, "hr@example.com", "hashed-password", Role.HR);
        when(credentialRepository.findByEmail("hr@example.com")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("Password123!", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(userId, "hr@example.com", "HR")).thenReturn("signed-jwt-token");

        LoginResponse response = authService.login(new LoginRequest("hr@example.com", "Password123!"));

        assertThat(response.token()).isEqualTo("signed-jwt-token");
    }

    @Test
    void shouldRejectUnknownEmail() {
        when(credentialRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldRejectWrongPassword() {
        Credential credential = new Credential(UUID.randomUUID(), "hr@example.com", "hashed-password", Role.HR);
        when(credentialRepository.findByEmail("hr@example.com")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("hr@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldRegisterNewCredential() {
        when(credentialRepository.existsByEmail("new-hr@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");

        RegisterCredentialResponse response = authService.registerCredential(
                new RegisterCredentialRequest("new-hr@example.com", "Password123!"));

        assertThat(response.userId()).isNotNull();
        verify(credentialRepository).save(any(Credential.class));
    }

    @Test
    void shouldRejectDuplicateEmailOnRegister() {
        when(credentialRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerCredential(
                new RegisterCredentialRequest("existing@example.com", "Password123!")))
                .isInstanceOf(DuplicateEmailException.class);

        verify(credentialRepository, never()).save(any());
    }

    @Test
    void shouldStoreHashedPasswordNotPlaintext() {
        when(credentialRepository.existsByEmail("hashed-check@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");

        authService.registerCredential(new RegisterCredentialRequest("hashed-check@example.com", "Password123!"));

        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }
}
