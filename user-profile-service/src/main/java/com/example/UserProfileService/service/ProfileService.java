package com.example.UserProfileService.service;

import com.example.UserProfileService.dto.ProfileResponse;
import com.example.UserProfileService.dto.UpdateProfileRequest;
import com.example.UserProfileService.entity.UserProfile;
import com.example.UserProfileService.exception.ProfileNotFoundException;
import com.example.UserProfileService.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Handles the authenticated "my profile" use cases (US-06, US-07). The
 * profile is always identified by the userId taken from the caller's JWT
 * (see {@code ProfileController}) - never from client-supplied input.
 */
@Service
public class ProfileService {

    private final UserProfileRepository userProfileRepository;

    public ProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public ProfileResponse getProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(ProfileNotFoundException::new);
        return toResponse(profile);
    }

    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(ProfileNotFoundException::new);

        profile.updateProfile(request.fullName(), request.phone());
        userProfileRepository.save(profile);

        return toResponse(profile);
    }

    private ProfileResponse toResponse(UserProfile profile) {
        return new ProfileResponse(
                profile.getUserId(), profile.getFullName(), profile.getEmail(),
                profile.getPhone(), profile.getCreatedAt(), profile.getUpdatedAt());
    }
}
