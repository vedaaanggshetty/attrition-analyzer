package com.example.AuthService.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialTest {

    @Test
    void shouldSetFieldsFromConstructor() {
        UUID userId = UUID.randomUUID();

        Credential credential = new Credential(userId, "hr@example.com", "hashed-password", Role.HR);

        assertThat(credential.getUserId()).isEqualTo(userId);
        assertThat(credential.getEmail()).isEqualTo("hr@example.com");
        assertThat(credential.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(credential.getRole()).isEqualTo(Role.HR);
    }

    @Test
    void shouldChangePasswordHash() {
        Credential credential = new Credential(UUID.randomUUID(), "hr@example.com", "old-hash", Role.HR);

        credential.changePassword("new-hash");

        assertThat(credential.getPasswordHash()).isEqualTo("new-hash");
    }
}
