package com.example.UserProfileService.exception;

/**
 * Thrown when the call to Authentication Service (via Feign) fails for a
 * reason other than a duplicate email - e.g. the service is unreachable or
 * returns an unexpected error. The message is intentionally generic so
 * internal details are never leaked to the client.
 */
public class AuthenticationServiceException extends RuntimeException {

    public AuthenticationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
