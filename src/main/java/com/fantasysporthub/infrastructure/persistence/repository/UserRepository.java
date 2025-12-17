package com.fantasysporthub.infrastructure.persistence.repository;

import com.fantasysporthub.domain.model.user.UserEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for User entity.
 */
public interface UserRepository extends R2dbcRepository<UserEntity, UUID> {

    /**
     * Find user by email for authentication.
     */
    Mono<UserEntity> findByEmail(String email);

    /**
     * Check if email already exists.
     */
    Mono<Boolean> existsByEmail(String email);

}
