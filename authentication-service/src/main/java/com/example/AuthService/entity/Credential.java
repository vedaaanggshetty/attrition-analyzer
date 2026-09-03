package com.example.AuthService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code credentials} table in {@code authentication_db}.
 *
 * Authentication owns only credential data (identity, password hash, role).
 * Profile information (name, phone, etc.) is owned by User Profile Service in
 * a separate database, linked only by this same {@code user_id} UUID - never
 * duplicated here.
 *
 * The primary key is generated in application code via {@code UUID.randomUUID()}
 * during registration, not by Hibernate/{@code @GeneratedValue}, because
 * Authentication must have the UUID in hand before it can return it to
 * User Profile Service.
 */
@Entity
@Table(name = "credentials")
public class Credential {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Credential() {
        // required by JPA/Hibernate
    }

    public Credential(UUID userId, String email, String passwordHash, Role role) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Replaces this credential's password hash (used by the password-reset
     * confirm flow). Always assign an already-BCrypt-hashed value here -
     * never a plaintext password.
     */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}
