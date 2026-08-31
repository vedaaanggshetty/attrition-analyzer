package com.example.AuthService.temporary;

/**
 * ============================================================
 * TEMPORARY in-memory user representation.
 * ------------------------------------------------------------
 * This exists only for the no-database development stage.
 * DELETE once the real Credential entity (Phase 3-4) is
 * implemented and replace usages with the persisted entity.
 * ============================================================
 */
public record TemporaryUser(String email, String passwordHash, String role) {
}
