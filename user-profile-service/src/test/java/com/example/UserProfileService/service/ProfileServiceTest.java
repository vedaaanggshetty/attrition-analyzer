package com.example.UserProfileService.service;

import com.example.UserProfileService.dto.ProfileResponse;
import com.example.UserProfileService.dto.UpdateProfileRequest;
import com.example.UserProfileService.entity.UserProfile;
import com.example.UserProfileService.exception.ProfileNotFoundException;
import com.example.UserProfileService.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    private UserProfileRepository userProfileRepository;
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        userProfileRepository = mock(UserProfileRepository.class);
        profileService = new ProfileService(userProfileRepository);
    }

    @Test
    void getProfile_withExistingUserId_returnsProfile() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId, "Jane HR", "hr@example.com", "555-1234");
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getProfile(userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.fullName()).isEqualTo("Jane HR");
        assertThat(response.email()).isEqualTo("hr@example.com");
    }

    @Test
    void getProfile_withUnknownUserId_throwsProfileNotFound() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(userId))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void updateProfile_withExistingUserId_updatesFullNameAndPhone() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId, "Old Name", "hr@example.com", "555-0000");
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.updateProfile(
                userId, new UpdateProfileRequest("New Name", "555-1111"));

        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(response.phone()).isEqualTo("555-1111");
        assertThat(response.email()).isEqualTo("hr@example.com");
        verify(userProfileRepository).save(profile);
    }

    @Test
    void updateProfile_withUnknownUserId_throwsProfileNotFound() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateProfile(
                userId, new UpdateProfileRequest("New Name", "555-1111")))
                .isInstanceOf(ProfileNotFoundException.class);
    }
}
