package com.fantasysporthub.domain.model.user;

import com.fantasysporthub.domain.model.ManagedPersistable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token entity for JWT token rotation and revocation.
 *
 * Design:
 * - Stores hashed token for security (never store plain tokens)
 * - Device fingerprint for fraud detection
 * - Revocation support for logout and security events
 * - Expiration tracking for automatic cleanup
 *
 * Security:
 * - Token hash instead of plain token storage
 * - Cascade delete when user is deleted
 * - Revocation audit trail with timestamp
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("refresh_tokens")
public class RefreshToken implements ManagedPersistable<UUID> {

    @Id
    private UUID id;

    /**
     * Transient flag to track if entity is new (not yet persisted).
     * Must be set to true when creating new entities, false when loading from DB.
     */
    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Column("user_id")
    private UUID userId;

    /**
     * SHA-256 hash of the refresh token.
     * Never store plain tokens in the database.
     */
    @Column("token_hash")
    private String tokenHash;

    /**
     * Device fingerprint for fraud detection.
     * Simplified in Phase 1, enhanced in Phase 5.
     */
    @Column("device_fingerprint")
    private String deviceFingerprint;

    /**
     * Token expiration timestamp.
     * Default: 30 days from creation.
     */
    @Column("expires_at")
    private Instant expiresAt;

    /**
     * Revocation status.
     * True when user logs out or token is compromised.
     */
    @Column("revoked")
    private boolean revoked;

    /**
     * Revocation timestamp for audit trail.
     * NULL when revoked = false.
     */
    @Column("revoked_at")
    private Instant revokedAt;

    /**
     * Token creation timestamp.
     */
    @Column("created_at")
    private Instant createdAt;

    /**
     * Check if token has expired.
     *
     * @return true if current time is after expiration
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if token is valid (not revoked and not expired).
     *
     * @return true if token can be used
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * Revoke this token.
     * Sets revoked flag and records timestamp.
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    @Override
    public void markAsExisting() {
        this.isNew = false;
    }
}
