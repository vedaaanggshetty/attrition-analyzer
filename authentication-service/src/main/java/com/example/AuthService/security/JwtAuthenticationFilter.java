package com.example.AuthService.security;

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

/**
 * Reads the Authorization header, validates a Bearer JWT via {@link JwtService},
 * and populates the {@link SecurityContextHolder} on success.
 *
 * On any failure (missing header, wrong scheme, invalid signature, expired
 * token, missing required claims) the filter simply leaves the request
 * unauthenticated and continues the chain - it never throws back to the
 * client. The eventual 401/403 is produced by Spring Security's normal
 * authorization rules, not by this filter.
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
        String email = claims.getSubject();
        Object roleClaim = claims.get(JwtService.ROLE_CLAIM);

        if (!StringUtils.hasText(email) || roleClaim == null || !StringUtils.hasText(roleClaim.toString())) {
            log.debug("JWT rejected: missing subject or role claim");
            return Optional.empty();
        }

        String authority = "ROLE_" + roleClaim;
        var token = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
        return Optional.of(token);
    }
}
