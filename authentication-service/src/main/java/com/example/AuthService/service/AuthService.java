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
import com.example.AuthService.security.JwtService;
import com.example.AuthService.security.TokenHashUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private static final long RESET_TOKEN_VALIDITY_MINUTES = 15;

    private final CredentialRepository credentialRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(CredentialRepository credentialRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.credentialRepository = credentialRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
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

    /**
     * Starts the password-reset flow (US-04) for the given email, if a
     * credential exists for it. Always completes normally either way - the
     * controller returns the same generic response regardless of whether a
     * token was actually created, so this endpoint never reveals which
     * emails are registered.
     *
     * Email delivery is not implemented yet: the raw token is generated and
     * only its hash is persisted, ready for a future email step to send the
     * raw value out-of-band. It is never logged or returned to the caller.
     */
    public void requestPasswordReset(PasswordResetRequestRequest request) {
        credentialRepository.findByEmail(request.email()).ifPresent(credential -> {
            String rawToken = TokenHashUtil.generateRawToken();
            String tokenHash = TokenHashUtil.sha256Hex(rawToken);
            Instant expiresAt = Instant.now().plus(RESET_TOKEN_VALIDITY_MINUTES, ChronoUnit.MINUTES);

            PasswordResetToken resetToken = new PasswordResetToken(
                    UUID.randomUUID(), credential.getUserId(), tokenHash, expiresAt);
            passwordResetTokenRepository.save(resetToken);
        });
    }

    /**
     * Completes the password-reset flow (US-04): validates the token
     * (exists, unused, unexpired), updates the credential's password hash,
     * and marks the token used so it cannot be replayed.
     */
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        String tokenHash = TokenHashUtil.sha256Hex(request.token());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .filter(t -> !t.isUsed())
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(InvalidResetTokenException::new);

        Credential credential = credentialRepository.findById(resetToken.getUserId())
                .orElseThrow(InvalidResetTokenException::new);

        credential.changePassword(passwordEncoder.encode(request.newPassword()));
        credentialRepository.save(credential);

        resetToken.markUsed();
        passwordResetTokenRepository.save(resetToken);
    }
}
