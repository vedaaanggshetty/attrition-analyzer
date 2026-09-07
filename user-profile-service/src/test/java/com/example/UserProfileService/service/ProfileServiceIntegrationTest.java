package com.example.UserProfileService.service;

import com.example.UserProfileService.dto.ProfileResponse;
import com.example.UserProfileService.dto.UpdateProfileRequest;
import com.example.UserProfileService.entity.UserProfile;
import com.example.UserProfileService.exception.ProfileNotFoundException;
import com.example.UserProfileService.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs against a real (in-memory H2) database, not mocks.
@SpringBootTest
@Transactional
class ProfileServiceIntegrationTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void shouldReturnPersistedProfile() {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(new UserProfile(userId, "Integration HR", "profile-get@example.com", "555-2222"));

        ProfileResponse response = profileService.getProfile(userId);

        assertThat(response.fullName()).isEqualTo("Integration HR");
    }

    @Test
    void shouldRejectUnknownUserId() {
        assertThatThrownBy(() -> profileService.getProfile(UUID.randomUUID()))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void shouldPersistProfileUpdate() {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(new UserProfile(userId, "Old Name", "profile-update@example.com", "555-0000"));

        profileService.updateProfile(userId, new UpdateProfileRequest("New Name", "555-3333"));

        UserProfile reloaded = userProfileRepository.findById(userId).orElseThrow();
        assertThat(reloaded.getFullName()).isEqualTo("New Name");
        assertThat(reloaded.getPhone()).isEqualTo("555-3333");
    }
}
