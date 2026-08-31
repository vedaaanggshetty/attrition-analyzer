package com.example.AuthService.service;

import com.example.AuthService.dto.LoginRequest;
import com.example.AuthService.dto.LoginResponse;
import com.example.AuthService.exception.InvalidCredentialsException;
import com.example.AuthService.security.JwtService;
import com.example.AuthService.temporary.TemporaryUser;
import com.example.AuthService.temporary.TemporaryUserStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final TemporaryUserStore temporaryUserStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(TemporaryUserStore temporaryUserStore,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.temporaryUserStore = temporaryUserStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        TemporaryUser user = temporaryUserStore.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.email(), user.role());
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs());
    }
}
