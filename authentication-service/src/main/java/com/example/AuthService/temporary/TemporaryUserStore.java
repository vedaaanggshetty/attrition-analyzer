package com.example.AuthService.temporary;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * ============================================================
 * TEMPORARY IN-MEMORY USER STORE
 * ------------------------------------------------------------
 * This class exists ONLY because MySQL/Authentication DB setup
 * has been intentionally postponed.
 *
 * It must be DELETED once the real Credential entity and
 * repository (Phase 3-4) are implemented. AuthService should
 * then be updated to use the repository instead of this store.
 *
 * Test account: email=hr@example.com / password=Password123!
 * ============================================================
 */
@Component
public class TemporaryUserStore {

    private final Map<String, TemporaryUser> usersByEmail;

    public TemporaryUserStore(PasswordEncoder passwordEncoder) {
        this.usersByEmail = Map.of(
                "hr@example.com",
                new TemporaryUser("hr@example.com", passwordEncoder.encode("Password123!"), "HR")
        );
    }

    public Optional<TemporaryUser> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }
}
