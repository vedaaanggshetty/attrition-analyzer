package com.example.UserProfileService.client;

import com.example.UserProfileService.dto.AuthCredentialRequest;
import com.example.UserProfileService.dto.AuthCredentialResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for Authentication Service's internal registration contract.
 * Resolved via Eureka using Authentication's {@code spring.application.name}
 * ({@code authentication-service}) - no hard-coded host/port.
 */
@FeignClient(name = "authentication-service")
public interface AuthenticationClient {

    @PostMapping("/internal/credentials")
    AuthCredentialResponse registerCredential(@RequestBody AuthCredentialRequest request);
}
