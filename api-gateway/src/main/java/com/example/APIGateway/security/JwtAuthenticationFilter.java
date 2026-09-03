package com.example.APIGateway.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the Authorization header, validates a Bearer JWT via
 * {@link JwtService}, and populates the {@link SecurityContextHolder} on
 * success. Mirrors authentication-service/user-profile-service's filter of
 * the same name - each service owns its own copy since there is no shared
 * library in this project.
 *
 * On any failure the filter simply leaves the request unauthenticated and
 * continues the chain - it never throws back to the client. The eventual
 * 401 is produced by Spring Security's normal authorization rules (see
 * {@code SecurityConfig}), not by this filter.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        extractToken(request)
                .flatMap(jwtService::parseClaims)
                .flatMap(this::toAuthentication)
                .ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? Optional.of(token) : Optional.empty();
    }

    private Optional<UsernamePasswordAuthenticationToken> toAuthentication(Claims claims) {
        Object emailClaim = claims.get(JwtService.EMAIL_CLAIM);
        Object roleClaim = claims.get(JwtService.ROLE_CLAIM);
        UUID userId = parseUserId(claims.getSubject());

        if (userId == null || emailClaim == null || !StringUtils.hasText(emailClaim.toString())
                || roleClaim == null || !StringUtils.hasText(roleClaim.toString())) {
            log.debug("JWT rejected: missing/invalid subject, email, or role claim");
            return Optional.empty();
        }

        String role = roleClaim.toString();
        AuthenticatedUser principal = new AuthenticatedUser(userId, emailClaim.toString(), role);
        var token = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        return Optional.of(token);
    }

    private UUID parseUserId(String subject) {
        if (!StringUtils.hasText(subject)) {
            return null;
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
