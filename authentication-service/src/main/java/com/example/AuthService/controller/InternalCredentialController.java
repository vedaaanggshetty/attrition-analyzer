package com.example.AuthService.controller;

import com.example.AuthService.dto.RegisterCredentialRequest;
import com.example.AuthService.dto.RegisterCredentialResponse;
import com.example.AuthService.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * INTERNAL service-to-service contract only.
 *
 * {@code POST /internal/credentials} is called by User Profile Service
 * (via Feign) during its public registration flow. It is NOT the frontend's
 * registration endpoint - there is no {@code POST /auth/register} here.
 */
@RestController
@RequestMapping("/internal/credentials")
public class InternalCredentialController {

    private final AuthService authService;

    public InternalCredentialController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<RegisterCredentialResponse> registerCredential(
            @Valid @RequestBody RegisterCredentialRequest request) {
        RegisterCredentialResponse response = authService.registerCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
