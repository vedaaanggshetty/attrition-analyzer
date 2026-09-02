package com.example.AuthService.dto;

/**
 * Generic success message body, used for endpoints (logout, password reset)
 * that don't need to return a domain object.
 */
public record MessageResponse(String message) {
}
