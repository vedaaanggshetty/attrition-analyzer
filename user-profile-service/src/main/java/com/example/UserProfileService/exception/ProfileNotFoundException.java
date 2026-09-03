package com.example.UserProfileService.exception;

/**
 * Thrown when a JWT references a userId with no corresponding profile row.
 * Should not occur in normal operation (a profile is always created
 * alongside its credential during registration) - this is a defensive
 * safeguard, not an expected user-facing flow.
 */
public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException() {
        super("Profile not found");
    }
}
