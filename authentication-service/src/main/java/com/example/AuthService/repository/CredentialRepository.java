package com.example.AuthService.repository;

import com.example.AuthService.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Credential}.
 *
 * Only the query methods actually needed by the current login flow (and the
 * upcoming registration flow) are declared here. Additional methods for
 * logout/password-reset will be added when those features are implemented.
 */
public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    Optional<Credential> findByEmail(String email);

    boolean existsByEmail(String email);
}
