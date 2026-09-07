package com.example.AuthService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issued JWT after a successful login")
public record LoginResponse(

        @Schema(description = "Signed JWT to send as a Bearer token on subsequent requests")
        String token,

        @Schema(description = "Token type to use in the Authorization header", example = "Bearer")
        String tokenType,

        @Schema(description = "Milliseconds until the token expires", example = "3600000")
        long expiresInMs
) {
}
