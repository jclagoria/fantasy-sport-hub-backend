package com.fantasysporthub.application.user.handlers;

import com.fantasysporthub.application.user.query.GetUserAuthenticationQuery;
import com.fantasysporthub.application.user.query.UserAuthenticationProjection;
import com.fantasysporthub.cqrs.query.QueryHandler;
import com.fantasysporthub.domain.util.RoleConverter;
import com.fantasysporthub.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for user authentication query.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthenticationQueryHandler implements QueryHandler<GetUserAuthenticationQuery, UserAuthenticationProjection> {

    private final UserRepository userRepository;

    @Override
    public Mono<UserAuthenticationProjection> handle(GetUserAuthenticationQuery query) {
        return userRepository.findById(query.userId())
                .map(userEntity -> UserAuthenticationProjection.builder()
                        .userId(userEntity.getId())
                        .email(userEntity.getEmail())
                        .displayName(userEntity.getDisplayName())
                        .roles(RoleConverter.jsonNodeToSet(userEntity.getRoles()))
                        .accountStatus(userEntity.getAccountStatus().name())
                        .build())
                .doOnSuccess(projection -> log.debug("Retrieved authentication data for user: {}", query.userId()))
                .switchIfEmpty(Mono.error(new UserNotFoundException(query.userId())));

    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(UUID userId) {
            super("User not found: " + userId);
        }
    }

}
