package com.example.AuthService.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // /auth/login, /auth/reset-password/** and actuator stay public.
    // /auth/logout requires a valid JWT (default "anyRequest().authenticated()"
    // rule) since it exists only to confirm a token was valid at call time.
    // Every other endpoint requires a valid JWT, validated by JwtAuthenticationFilter.
    //
    // TEMPORARY INTERNAL-ACCESS ASSUMPTION: /internal/** (currently just
    // POST /internal/credentials) is permitted without a JWT because it is
    // meant for service-to-service calls (e.g. User Profile Service via
    // Feign), not the public frontend, and no service-to-service auth
    // mechanism (mTLS, shared internal token, network policy, etc.) exists
    // in this project yet. This must be revisited before any real deployment
    // - anyone who can reach this service's port can currently call it.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/auth/reset-password/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
