package com.fantasysporthub.infrastructure.persistence.repository;

import com.fantasysporthub.domain.model.user.RefreshToken;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface RefreshTokenRepository extends R2dbcRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by hash.
     */
    Mono<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find all tokens for a user.
     */
    Flux<RefreshToken> findByUserId(UUID userId);

    /**
     * Delete expired tokens (cleanup job).
     */
    @Query("DELETE FROM refresh_tokens WHERE expires_at < :now OR REVOKED = TRUE")
    Mono<Integer> deleteExpiredAndRevoked(Instant now);

}
