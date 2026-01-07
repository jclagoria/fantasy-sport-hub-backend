package com.fantasysporthub.presentation.controller;

import com.fantasysporthub.application.user.command.LoginCommand;
import com.fantasysporthub.application.user.command.LogoutCommand;
import com.fantasysporthub.application.user.command.RegisterUserCommand;
import com.fantasysporthub.application.user.handlers.UserCommandHandler;
import com.fantasysporthub.application.user.query.ValidateTokenQuery;
import com.fantasysporthub.cqrs.command.CommandBus;
import com.fantasysporthub.cqrs.query.QueryBus;
import com.fantasysporthub.presentation.dto.LoginRequest;
import com.fantasysporthub.presentation.dto.RegisterRequest;
import com.fantasysporthub.presentation.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

/**
 * Authentication REST Controller following CQRS pattern.
 *
 * CQRS Implementation:
 * - Commands (writes) → CommandBus → CommandHandler → EventStore + PostgreSQL
 * - Queries (reads) → QueryBus → QueryHandler → Read Models (MongoDB/PostgreSQL)
 *
 * This controller is a thin adapter that:
 * 1. Converts HTTP requests to Commands/Queries
 * 2. Dispatches to appropriate bus
 * 3. Converts results back to HTTP responses
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    /**
     * POST /auth/register
     * Register new user (COMMAND - Write operation).
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            ServerHttpRequest httpRequest
    ) {
        var command = RegisterUserCommand.builder()
                .commandId(UUID.randomUUID())
                .email(request.email())
                .password(request.password())
                .displayName(request.displayName())
                .ipAddress(extractIpAddress(httpRequest))
                .userAgent(extractUserAgent(httpRequest))
                .build();

        // Dispatch command through CommandBus (CQRS pattern)
        return commandBus.dispatch(command)
                .map(userId -> new RegisterResponse((UUID) userId, request.email()));
    }

    /**
     * POST /auth/login
     * Authenticate user and return tokens (COMMAND - Write operation).
     */
    @PostMapping("/login")
    public Mono<TokenResponse> login (
            @Valid @RequestBody LoginRequest request,
            ServerHttpRequest httpRequest
            ) {
        var command = LoginCommand.builder()
                .commandId(UUID.randomUUID())
                .email(request.email())
                .password(request.password())
                .deviceFingerprint(generateDeviceFingerprint(httpRequest))
                .ipAddress(extractIpAddress(httpRequest))
                .userAgent(extractUserAgent(httpRequest))
                .build();

        // Dispatch command through CommandBus (CQRS pattern)
        return commandBus.<LoginCommand, UserCommandHandler.LoginResponse>dispatch(command)
                .map(response -> new TokenResponse(
                        response.accessToken(),
                        response.refreshToken(),
                        3600, // 1 hour in seconds
                        "Bearer",
                        response.userId(),
                        response.email(),
                        response.displayName()
                ));
    }

    /**
     * POST /auth/logout
     * Revoke refresh token (COMMAND - Write operation).
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(@RequestBody LogoutRequest request) {
        var command = LogoutCommand.builder()
                .commandId(UUID.randomUUID())
                .refreshToken(request.refreshToken())
                .build();

        return commandBus.dispatch(command);
    }

    /**
     * GET /auth/validate
     * Validate token (QUERY - Read operation).
     */
    @GetMapping("/validate")
    public Mono<ValidationResponse> validateToken(
            @RequestHeader("Authorization") String authHeader
    ) {
        var token = authHeader.replace("Bearer ", "");
        var query = new ValidateTokenQuery(token);

        return queryBus.dispatch(query)
                .map(claims -> new ValidationResponse(
                        true,
                        claims.userId(),
                        claims.email(),
                        claims.roles()
                ));
    }

    // Helper methods
    private String extractIpAddress(ServerHttpRequest request) {
        var xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        var remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    private String extractUserAgent(ServerHttpRequest request) {
        var userAgent = request.getHeaders().getFirst("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }

    private String generateDeviceFingerprint(ServerHttpRequest request) {
        // Simplified fingerprint (Phase 1)
        var ip = extractIpAddress(request);
        var userAgent = extractUserAgent(request);
        return Integer.toHexString((userAgent + ip).hashCode());
    }

    // Response DTOs
    public record RegisterResponse(UUID userId, String email) {}
    public record LogoutRequest(String refreshToken) {}
    public record ValidationResponse(
            boolean isValid,
            UUID userId,
            String email,
            Set<String> roles
    ) {}
}
