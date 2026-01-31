package com.fantasysporthub.application.user.handlers;

import com.fantasysporthub.application.user.query.GetUserByEmailQuery;
import com.fantasysporthub.application.user.query.UserProjectionDTO;
import com.fantasysporthub.application.user.query.ValidateTokenQuery;
import com.fantasysporthub.domain.service.JWTService;
import com.fantasysporthub.domain.util.RoleConverter;
import com.fantasysporthub.infrastructure.persistence.repository.UserRepository;
import com.fantasysporthub.security.JWTClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Query Handler for user authentication queries.
 * <p>
 * CQRS Read Side:
 * - Reads from optimized read models
 * - No side effects
 * - Fast, cacheable queries
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserQueryHandler {

    private final UserRepository userRepository;
    private final JWTService jwtService;

    /**
     * Handle GetUserByEmailQuery.
     * Reads from PostgreSQL read model.
     */
    public Mono<UserProjectionDTO> handle(GetUserByEmailQuery query) {
        return userRepository.findByEmail(query.email())
                .map(user -> new UserProjectionDTO(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        RoleConverter.jsonNodeToSet(user.getRoles()),
                        user.getAccountStatus().name(),
                        user.getCreatedAt(),
                        user.getLastLoginAt()
                ))
                .doOnSuccess(projection -> log.debug("User projection retrieved: {}", query.email()))
                .doOnError(e -> log.error("Failed to get user projection: {}", query.email(), e));
    }

    /**
     * Handle ValidateTokenQuery.
     * Pure validation, no database access.
     */
    public Mono<JWTClaims> handle(ValidateTokenQuery query) {
        return jwtService.validateToken(query.token())
                .doOnSuccess(claims -> log.debug("Token validated for user: {}", claims.email()))
                .doOnError(e -> log.warn("Token validation failed", e));
    }

}
