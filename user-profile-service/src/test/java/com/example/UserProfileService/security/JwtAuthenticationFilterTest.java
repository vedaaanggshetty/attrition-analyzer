package com.example.UserProfileService.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthenticationFilter(jwtService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_withValidToken_setsAuthenticationWithAuthenticatedUserPrincipal() throws Exception {
        UUID userId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get(JwtService.EMAIL_CLAIM)).thenReturn("hr@example.com");
        when(claims.get(JwtService.ROLE_CLAIM)).thenReturn("HR");

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.parseClaims("valid-token")).thenReturn(Optional.of(claims));

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthenticatedUser(userId, "hr@example.com", "HR"));
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_HR");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withMissingAuthorizationHeader_doesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withMalformedScheme_doesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withInvalidOrExpiredToken_doesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtService.parseClaims("invalid-token")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withMissingRoleClaim_doesNotAuthenticate() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(claims.get(JwtService.EMAIL_CLAIM)).thenReturn("hr@example.com");
        when(claims.get(JwtService.ROLE_CLAIM)).thenReturn(null);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.parseClaims("valid-token")).thenReturn(Optional.of(claims));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withNonUuidSubject_doesNotAuthenticate() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("hr@example.com");
        when(claims.get(JwtService.EMAIL_CLAIM)).thenReturn("hr@example.com");
        when(claims.get(JwtService.ROLE_CLAIM)).thenReturn("HR");

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.parseClaims("valid-token")).thenReturn(Optional.of(claims));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
