package com.fantasysporthub.application.user.handlers;

import com.fantasysporthub.application.user.command.LoginCommand;
import com.fantasysporthub.application.user.command.LogoutCommand;
import com.fantasysporthub.application.user.command.RegisterUserCommand;
import com.fantasysporthub.cqrs.event.EventBus;
import com.fantasysporthub.domain.model.Role;
import com.fantasysporthub.domain.model.user.Email;
import com.fantasysporthub.domain.model.user.Password;
import com.fantasysporthub.domain.model.user.UserEntity;
import com.fantasysporthub.domain.service.JWTService;
import com.fantasysporthub.domain.service.PasswordService;
import com.fantasysporthub.domain.service.RefreshTokenService;
import com.fantasysporthub.domain.user.event.LoginFailed;
import com.fantasysporthub.domain.user.event.LoginSuccessful;
import com.fantasysporthub.domain.user.event.UserRegistered;
import com.fantasysporthub.domain.util.RoleConverter;
import com.fantasysporthub.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Handler for user authentication commands.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCommandHandler {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EventBus eventBus;

    /**
     * Handle user registration.
     */
    public Mono<UUID> handleRegister(RegisterUserCommand command) {
        return Mono.just(command)
                // 1. Validate email format
                .map(cmd -> new Email(cmd.email()))
                .map(email -> {
                    new Password(command.password()); // Throws if invalid
                    return email;
                })
                // 3. Check email uniqueness
                .flatMap(email -> userRepository.existsByEmail(email.getValue())
                        .flatMap(exists -> exists
                                ? Mono.error(new EmailAlreadyExistsException(email.getValue()))
                                : Mono.just(email)
                        )
                )
                // 4. Hash password
                .flatMap(email -> passwordService
                        .hashPassword(command.password())
                        .map(hash -> new UserCreationData(email.getValue(), hash))
                )
                // 5. Create and save user
                .flatMap(data -> {
                    return Mono.zip(
                            Mono.fromCallable(UUID::randomUUID), // Diferir generación de UUID
                            Mono.just(Instant.now()), // Aunque es rápido, lo mantenemos en el contexto reactivo
                            Mono.just(RoleConverter.collectionToJsonNode(Set.of(Role.USER.toString())))
                    ).flatMap(tuple -> {
                        var userId = tuple.getT1();
                        var now = tuple.getT2();
                        var roles = tuple.getT3();

                        var user = UserEntity.builder()
                                .id(userId)
                                .email(data.email)
                                .passwordHash(data.passwordHash)
                                .displayName(command.displayName())
                                .roles(roles)
                                .processingRestricted(false)
                                .accountStatus(UserEntity.AccountStatus.ACTIVE)
                                .createdAt(now)
                                .build();
                        return userRepository.save(user);
                    });
                })
                // 6. Emit UserRegistered event
                .flatMap(user -> {
                    var event = UserRegistered.builder()
                            .eventId(UUID.randomUUID())
                            .userId(user.getId())
                            .email(user.getEmail())
                            .displayName(user.getDisplayName())
                            .roles(RoleConverter.jsonNodeToSet(user.getRoles()))
                            .occurredAt(Instant.now())
                            .ipAddress(command.ipAddress())
                            .userAgent(command.userAgent())
                            .build();

                    return eventBus.publish(event)
                            .thenReturn(user.getId());
                })
                .doOnSuccess(userId -> log.info("User registered successfully: {}", userId))
                .doOnError(e -> log.error("Registration failed for email: {}", command.email(), e));

    }

    /**
     * Handle login command.
     */
    public Mono<LoginResponse> handleLogin(LoginCommand command) {
        return Mono.just(command)
                // 1. Find user by email
                .flatMap(cmd -> userRepository.findByEmail(cmd.email())
                        .switchIfEmpty(Mono.defer(() ->
                                emitLoginFailed(cmd.email(), "User not found", cmd)
                                        .then(Mono.error(new InvalidCredentialsException()))
                        ))
                )
                // 2. Verify password
                .flatMap(user -> passwordService.verifyPassword(command.password(), user.getPasswordHash())
                        .flatMap(valid -> valid
                                ? Mono.just(user)
                                : emitLoginFailed(command.email(), "Invalid password", command)
                                .then(Mono.error(new InvalidCredentialsException()))
                        )
                )
                // 3. Check account status
                .flatMap(user -> {
                    if (user.getAccountStatus() != UserEntity.AccountStatus.ACTIVE) {
                        return Mono.error(new AccountSuspendedException());
                    }
                    return Mono.just(user);
                })
                // 4. Generate tokens
                .flatMap(user -> Mono.zip(
                        jwtService.generateAccessToken(user, command.deviceFingerprint()),
                        refreshTokenService.generateRefreshToken(user.getId(), command.deviceFingerprint()),
                        Mono.just(user)
                ))
                // 5. Update lastLoginAt
                .flatMap(tuple -> {
                    var accessToken = tuple.getT1();
                    var refreshToken = tuple.getT2();
                    var user = tuple.getT3();

                    user.setLastLoginAt(Instant.now());
                    return userRepository.save(user)
                            .thenReturn(new TokenData(accessToken, refreshToken, user));
                })
                // 6. Emit LoginSuccessful event
                .flatMap(data -> {
                    var event = LoginSuccessful.builder()
                            .eventId(UUID.randomUUID())
                            .userId(data.user.getId())
                            .email(data.user.getEmail())
                            .occurredAt(Instant.now())
                            .ipAddress(command.ipAddress())
                            .userAgent(command.userAgent())
                            .deviceFingerprint(command.deviceFingerprint())
                            .build();

                    return eventBus.publish(event)
                            .thenReturn(new LoginResponse(
                                    data.accessToken,
                                    data.refreshToken,
                                    data.user.getId(),
                                    data.user.getEmail(),
                                    data.user.getDisplayName(),
                                    RoleConverter.jsonNodeToSet(data.user.getRoles())
                            ));
                })
                .doOnSuccess(response -> log.info("User logged in: {}", response.email()))
                .doOnError(e -> log.error("Login failed for: {}", command.email(), e));
    }

    /**
     * Handle logout command.
     */
    public Mono<Void> handleLogout(LogoutCommand command) {
        return refreshTokenService.revokeToken(command.refreshToken())
                .doOnSuccess(unused -> log.info("User logged out, refresh token revoked"))
                .doOnError(e -> log.error("Failed to revoke refresh token during logout", e));
    }

    /**
     * Emit LoginFailed event.
     */
    private Mono<Void> emitLoginFailed(String email, String reason, LoginCommand command) {
        var event = LoginFailed.builder()
                .eventId(UUID.randomUUID())
                .email(email)
                .reason(reason)
                .occurredAt(Instant.now())
                .ipAddress(command.ipAddress())
                .userAgent(command.userAgent())
                .build();

        return eventBus.publish(event);
    }

    // Internal DTOs
    private record UserCreationData(String email, String passwordHash) {
    }

    private record TokenData(String accessToken, String refreshToken, UserEntity user) {
    }

    // Response DTO
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            UUID userId,
            String email,
            String displayName,
            Set<String> roles
    ) {
    }

    // Exceptions
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) {
            super("Email already exists: " + email);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Invalid email or password.");
        }
    }

    public static class AccountSuspendedException extends RuntimeException {
        public AccountSuspendedException() {
            super("Account is suspended");
        }
    }

}
