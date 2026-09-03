package com.example.AuthService.exception;

/**
 * Thrown when a password-reset confirmation is attempted with a token that
 * is unknown, expired, or already used. The message is intentionally
 * generic - it never distinguishes between those cases to a caller.
 */
public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super("Invalid or expired reset token");
    }
}
