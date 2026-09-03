package com.example.UserProfileService.repository;

import com.example.UserProfileService.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UserProfile}.
 *
 * Only the query methods actually needed by the current registration flow
 * are declared here.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    boolean existsByEmail(String email);
}
