package com.example.UserProfileService.service;

import com.example.UserProfileService.client.AuthenticationClient;
import com.example.UserProfileService.dto.AuthCredentialRequest;
import com.example.UserProfileService.dto.AuthCredentialResponse;
import com.example.UserProfileService.dto.RegisterUserRequest;
import com.example.UserProfileService.dto.RegisterUserResponse;
import com.example.UserProfileService.entity.UserProfile;
import com.example.UserProfileService.exception.AuthenticationServiceException;
import com.example.UserProfileService.exception.DuplicateEmailException;
import com.example.UserProfileService.repository.UserProfileRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {

    private final UserProfileRepository userProfileRepository;
    private final AuthenticationClient authenticationClient;

    public UserRegistrationService(UserProfileRepository userProfileRepository,
                                    AuthenticationClient authenticationClient) {
        this.userProfileRepository = userProfileRepository;
        this.authenticationClient = authenticationClient;
    }

    /**
     * Coordinates the public registration flow (US-01):
     * <ol>
     *   <li>Fail fast if this service already has a profile for the email
     *       (avoids creating an orphan credential in Authentication).</li>
     *   <li>Call Authentication Service (via Feign) to create the credential
     *       and obtain the generated {@code userId}.</li>
     *   <li>Save the profile locally using that same {@code userId} as the
     *       primary key.</li>
     * </ol>
     * The password is never persisted in User Profile's own database.
     */
    public RegisterUserResponse register(RegisterUserRequest request) {
        if (userProfileRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        AuthCredentialResponse credentialResponse;
        try {
            credentialResponse = authenticationClient.registerCredential(
                    new AuthCredentialRequest(request.email(), request.password()));
        } catch (FeignException.Conflict ex) {
            throw new DuplicateEmailException();
        } catch (FeignException ex) {
            throw new AuthenticationServiceException(
                    "Unable to complete registration - authentication service error", ex);
        }

        UserProfile profile = new UserProfile(
                credentialResponse.userId(), request.fullName(), request.email(), request.phone());
        profile = userProfileRepository.save(profile);

        return new RegisterUserResponse(
                profile.getUserId(), profile.getFullName(), profile.getEmail(),
                profile.getPhone(), profile.getCreatedAt());
    }
}
