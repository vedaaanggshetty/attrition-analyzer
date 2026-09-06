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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(CredentialRepository credentialRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Credential credential = credentialRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(credential.getUserId(), credential.getEmail(), credential.getRole().name());
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs());
    }

    /**
     * Creates a new credential record for the internal registration contract
     * ({@code POST /internal/credentials}). Called by User Profile Service
     * (via Feign) during the public registration flow - not called directly
     * by the frontend.
     */
    public RegisterCredentialResponse registerCredential(RegisterCredentialRequest request) {
        if (credentialRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        UUID userId = UUID.randomUUID();
        String passwordHash = passwordEncoder.encode(request.password());
        Credential credential = new Credential(userId, request.email(), passwordHash, Role.HR);

        credentialRepository.save(credential);

        return new RegisterCredentialResponse(userId);
    }
}
