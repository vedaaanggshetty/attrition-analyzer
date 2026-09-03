package com.example.AuthService.exception;

/**
 * Thrown when login fails due to an unknown email or a non-matching
 * password. The message is intentionally generic so the API response
 * never reveals which of the two was incorrect.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
