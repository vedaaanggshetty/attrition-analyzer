package com.example.UserProfileService.exception;

/**
 * Thrown when registration is attempted for an email that already has a
 * stored profile, or when Authentication Service reports the email is
 * already registered as a credential.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("Email is already registered");
    }
}
