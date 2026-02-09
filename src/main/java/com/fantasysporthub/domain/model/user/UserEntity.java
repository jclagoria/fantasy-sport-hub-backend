package com.fantasysporthub.domain.model.user;

import com.fantasysporthub.domain.model.ManagedPersistable;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * User entity for authentication and authorization.
 *
 * GDPR Compliance: Minimal data collection (email, password hash, roles)
 *
 * Implements Persistable to control new/existing entity detection for R2DBC.
 * This is required when using application-generated UUIDs as primary keys,
 * otherwise R2DBC would attempt UPDATE instead of INSERT for new entities.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class UserEntity implements ManagedPersistable<UUID> {

    @Id
    private UUID id;

    /**
     * Transient flag to track if entity is new (not yet persisted).
     * Must be set to true when creating new entities, false when loading from DB.
     */
    @Transient
    @Builder.Default
    private boolean isNew = true;

    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Column("display_name")
    private String displayName;

    @Column("phone_number")
    private String phone_number;

    @Column("date_of_birth")
    private LocalDate dateOfBirth;

    private JsonNode roles;

    @Column("processing_restricted")
    private boolean processingRestricted;

    @Column("restriction_reason")
    private String restrictionReason;

    @Column("restricted_at")
    private Instant restrictedAt;

    @Column("deleted_at")
    private Instant deletedAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("last_login_at")
    private Instant lastLoginAt;

    @Column("account_status")
    private AccountStatus accountStatus;

    public enum AccountStatus {
        ACTIVE,
        SUSPENDED,
        DELETED
    }

    /**
     * Implementation of Persistable interface.
     * Returns true if entity is new (needs INSERT), false if existing (needs UPDATE).
     */
    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public void markAsExisting() {
        this.isNew = false;
    }
}
