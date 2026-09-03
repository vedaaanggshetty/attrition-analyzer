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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test proving {@link ProfileService} reads/writes the real
 * {@code profiles} table in MySQL (not a mock repository). Each test seeds
 * its own row inside a transaction that Spring's test framework rolls back
 * afterward, so no manual cleanup is needed.
 */
@SpringBootTest
@Transactional
class ProfileServiceIntegrationTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void getProfile_withPersistedProfile_returnsMatchingData() {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(new UserProfile(userId, "Integration HR", "profile-get@example.com", "555-2222"));

        ProfileResponse response = profileService.getProfile(userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.fullName()).isEqualTo("Integration HR");
        assertThat(response.email()).isEqualTo("profile-get@example.com");
    }

    @Test
    void getProfile_withUnknownUserId_throwsProfileNotFound() {
        assertThatThrownBy(() -> profileService.getProfile(UUID.randomUUID()))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void updateProfile_persistsChangesToRealDatabase() {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(new UserProfile(userId, "Old Name", "profile-update@example.com", "555-0000"));

        profileService.updateProfile(userId, new UpdateProfileRequest("New Name", "555-3333"));

        Optional<UserProfile> reloaded = userProfileRepository.findById(userId);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getFullName()).isEqualTo("New Name");
        assertThat(reloaded.get().getPhone()).isEqualTo("555-3333");
        assertThat(reloaded.get().getEmail()).isEqualTo("profile-update@example.com");
    }
}
