package com.example.EmployeeService.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.EmployeeService.exception.UnauthenticatedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Verifies JWTs issued by Authentication Service and reads the caller's email
 * out of them. Employee Service only needs to know which HR user is flagging
 * an employee, not issue or refresh tokens, so this is intentionally just a
 * reader (mirrors Notification Service's JwtService).
 */
@Component
public class JwtService {

	private final SecretKey signingKey;

	public JwtService(@Value("${jwt.secret}") String secret) {
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	private static final String EMAIL_CLAIM = "email";

	public String extractEmail(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return claims.get(EMAIL_CLAIM, String.class);
		} catch (JwtException | IllegalArgumentException ex) {
			throw new UnauthenticatedException();
		}
	}
}
