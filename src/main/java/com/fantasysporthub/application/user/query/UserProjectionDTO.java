package com.fantasysporthub.application.user.query;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Read model projection for user queries.
 * Optimized for fast reads from MongoDB (future) or PostgreSQL.
 */
public record UserProjectionDTO(
        UUID userId,
        String email,
        String displayName,
        Set<String> roles,
        String accountStatus,
        Instant createdAt,
        Instant lastLoginAt
) {
}
