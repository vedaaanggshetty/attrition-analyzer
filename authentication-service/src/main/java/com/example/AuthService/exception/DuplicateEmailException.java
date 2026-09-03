package com.example.AuthService.exception;

/**
 * Thrown when credential registration is attempted for an email that
 * already has a stored credential.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("Email is already registered");
    }
}
