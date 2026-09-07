package com.example.NotificationService.client;

import com.example.NotificationService.dto.InternalProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for User Profile Service's internal profile-lookup contract.
 * Resolved via Eureka using User Profile's {@code spring.application.name}
 * ({@code user-profile-service}) - no hard-coded host/port.
 *
 * Used only to resolve a display name for legacy notifications whose
 * hr_user_email column holds a userId instead of an email address (see
 * NotificationService.resolveSenderName) - not part of the normal
 * create/list notification flow.
 */
@FeignClient(name = "user-profile-service")
public interface UserProfileClient {

    @GetMapping("/internal/profiles/{userId}")
    InternalProfileResponse getProfile(@PathVariable("userId") String userId);
}
