package com.example.AuthService.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test for {@link Credential} - no Spring context, no database.
 * Repository/persistence-level testing is deferred until the phase where
 * MySQL is officially introduced.
 */
class CredentialTest {

    @Test
    void constructor_setsAllProvidedFields() {
        UUID userId = UUID.randomUUID();

        Credential credential = new Credential(userId, "hr@example.com", "hashed-password", Role.HR);

        assertThat(credential.getUserId()).isEqualTo(userId);
        assertThat(credential.getEmail()).isEqualTo("hr@example.com");
        assertThat(credential.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(credential.getRole()).isEqualTo(Role.HR);
    }

    @Test
    void createdAt_isNullUntilPersisted() {
        Credential credential = new Credential(UUID.randomUUID(), "hr@example.com", "hashed-password", Role.HR);

        // @CreationTimestamp is populated by Hibernate at insert time, not by the constructor.
        assertThat(credential.getCreatedAt()).isNull();
    }

    @Test
    void changePassword_replacesStoredPasswordHash() {
        Credential credential = new Credential(UUID.randomUUID(), "hr@example.com", "old-hash", Role.HR);

        credential.changePassword("new-hash");

        assertThat(credential.getPasswordHash()).isEqualTo("new-hash");
    }
}
