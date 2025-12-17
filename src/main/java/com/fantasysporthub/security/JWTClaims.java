package com.fantasysporthub.security;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * JWT token claims structure.
 */
public record JWTClaims(
        UUID userId,
        String email,
        Set<String> roles,
        Map<String, Object> attributes,
        Instant issuedAt,
        Instant expiresAt,
        String tokenId,
        String deviceFingerprint // Phase 1: Fraud detection
) {
}
