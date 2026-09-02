package com.example.AuthService.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Utility for generating and hashing password-reset tokens.
 *
 * Raw tokens are never persisted - only their SHA-256 hash is stored, the
 * same way a stolen database dump can't be used to log in with a stolen
 * password hash. SHA-256 (not BCrypt) is used here deliberately: the token
 * is already a high-entropy random value (not a human-chosen password), so
 * a slow adaptive hash provides no additional benefit and would only slow
 * down the (public, unauthenticated) confirm endpoint.
 */
public final class TokenHashUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenHashUtil() {
    }

    public static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is guaranteed to be available on every JDK - this is unreachable.
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
