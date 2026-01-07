-- =====================================================
-- Refresh Tokens Table Migration
-- =====================================================
-- Purpose: Store refresh tokens for JWT token rotation
-- Author: Phase 1 MVP - Authentication Service
-- Date: 2025-12-29
-- =====================================================

-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    -- Primary key
    id UUID PRIMARY KEY,

    -- Foreign key to users table
    user_id UUID NOT NULL,

    -- Token storage (SHA-256 hash, never store plain tokens)
    token_hash VARCHAR(255) NOT NULL,

    -- Device identification for fraud detection
    device_fingerprint VARCHAR(255),

    -- Expiration management
    expires_at TIMESTAMP NOT NULL,

    -- Revocation management
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,

    -- Audit timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Foreign key constraint
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    -- Business logic constraint: revoked_at must be NULL when revoked = FALSE
    CONSTRAINT chk_refresh_tokens_revocation
        CHECK (
            (revoked = FALSE AND revoked_at IS NULL)
            OR (revoked = TRUE AND revoked_at IS NOT NULL)
        )
);

-- =====================================================
-- Indexes for Performance
-- =====================================================

-- Index for token lookup (most frequent operation)
CREATE UNIQUE INDEX idx_refresh_tokens_token_hash
    ON refresh_tokens(token_hash);

-- Index for user token management
CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

-- Index for expiration queries
CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens(expires_at);

-- Composite index for active token queries (optimized for WHERE clauses)
CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens (user_id, expires_at)
    WHERE revoked = FALSE;

-- Partial index for cleanup operations (expired or revoked tokens)
CREATE INDEX idx_refresh_tokens_revoked
    ON refresh_tokens (expires_at)
    WHERE revoked = TRUE;

-- Index for audit queries (by creation date)
CREATE INDEX idx_refresh_tokens_created_at
    ON refresh_tokens(created_at);

-- =====================================================
-- Comments for Documentation
-- =====================================================

COMMENT ON TABLE refresh_tokens IS 'Stores refresh tokens for JWT authentication with rotation support';
COMMENT ON COLUMN refresh_tokens.id IS 'Unique identifier for the refresh token record';
COMMENT ON COLUMN refresh_tokens.user_id IS 'Reference to the user who owns this token';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of the refresh token (never store plain tokens)';
COMMENT ON COLUMN refresh_tokens.device_fingerprint IS 'Device identifier for fraud detection and device management';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Token expiration timestamp (default: 30 days from creation)';
COMMENT ON COLUMN refresh_tokens.revoked IS 'Revocation status (true when user logs out or token is compromised)';
COMMENT ON COLUMN refresh_tokens.revoked_at IS 'Timestamp when token was revoked (NULL when revoked = false)';
COMMENT ON COLUMN refresh_tokens.created_at IS 'Token creation timestamp';

-- =====================================================
-- Migration Notes
-- =====================================================
-- 1. Token Storage:
--    - Stores SHA-256 hash, not plain tokens
--    - Prevents token theft from database breach
--
-- 2. Cascade Delete:
--    - When user is deleted, all refresh tokens are deleted
--    - Maintains referential integrity
--
-- 3. Performance:
--    - Token lookup: O(1) with unique index on token_hash
--    - User tokens: O(log n) with index on user_id
--    - Cleanup: Efficient with partial index
--
-- 4. Security:
--    - Revocation support for logout
--    - Device fingerprint for fraud detection
--    - Audit trail with revoked_at timestamp
--
-- 5. Maintenance:
--    - Scheduled cleanup job should run daily
--    - Delete expired/revoked tokens older than 90 days
--    - Monitor index bloat and rebuild if needed
--
-- 6. GDPR Compliance:
--    - Tokens deleted when user is deleted (CASCADE)
--    - Retention policy: cleanup after 90 days
--    - No PII stored in token_hash or device_fingerprint
