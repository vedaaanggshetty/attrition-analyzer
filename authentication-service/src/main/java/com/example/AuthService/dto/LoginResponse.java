package com.example.AuthService.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInMs
) {
}
