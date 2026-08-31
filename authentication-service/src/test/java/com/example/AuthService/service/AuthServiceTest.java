package com.example.AuthService.service;

import com.example.AuthService.dto.LoginRequest;
import com.example.AuthService.dto.LoginResponse;
import com.example.AuthService.exception.InvalidCredentialsException;
import com.example.AuthService.security.JwtService;
import com.example.AuthService.temporary.TemporaryUser;
import com.example.AuthService.temporary.TemporaryUserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private TemporaryUserStore temporaryUserStore;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        temporaryUserStore = mock(TemporaryUserStore.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(temporaryUserStore, passwordEncoder, jwtService);
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        TemporaryUser user = new TemporaryUser("hr@example.com", "hashed-password", "HR");
        LoginRequest request = new LoginRequest("hr@example.com", "Password123!");

        when(temporaryUserStore.findByEmail("hr@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("hr@example.com", "HR")).thenReturn("signed-jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("signed-jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMs()).isEqualTo(3600000L);
    }

    @Test
    void login_withUnknownEmail_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("unknown@example.com", "whatever");

        when(temporaryUserStore.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentials() {
        TemporaryUser user = new TemporaryUser("hr@example.com", "hashed-password", "HR");
        LoginRequest request = new LoginRequest("hr@example.com", "wrong-password");

        when(temporaryUserStore.findByEmail("hr@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
