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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "User authentication and authorization endpoints using JWT tokens and CQRS pattern")
public class AuthController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    /**
     * POST /auth/register
     * Register new user (COMMAND - Write operation).
     */
    @Operation(
            summary = "Register new user",
            description = """
                    Creates a new user account with email and password authentication.

                    **CQRS Command**: RegisterUserCommand → CommandBus → UserCommandHandler → EventStore

                    **Security**: Passwords are hashed with Argon2id before storage.
                    **Event Sourcing**: Publishes UserRegistered event to EventStoreDB.
                    """,
            security = {}  // No authentication required for registration
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User successfully registered",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegisterResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful Registration",
                                    value = """
                                            {
                                              "userId": "550e8400-e29b-41d4-a716-446655440000",
                                              "email": "user@example.com"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input (validation errors)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = """
                                            {
                                              "timestamp": "2025-01-07T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Email is required"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User with this email already exists",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegisterResponse> register(
            @Parameter(description = "User registration details", required = true)
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
    @Operation(
            summary = "Authenticate user and obtain JWT tokens",
            description = """
                    Authenticates user credentials and returns access and refresh tokens.

                    **CQRS Command**: LoginCommand → CommandBus → UserCommandHandler → EventStore

                    **Returns**:
                    - Access Token (JWT, 1 hour expiration)
                    - Refresh Token (stored in PostgreSQL, 7 days expiration)

                    **Event Sourcing**: Publishes LoginSuccessful or LoginFailed events.
                    """,
            security = {}  // No authentication required for login
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful, tokens returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful Login",
                                    value = """
                                            {
                                              "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
                                              "expiresIn": 3600,
                                              "tokenType": "Bearer",
                                              "userId": "550e8400-e29b-41d4-a716-446655440000",
                                              "email": "user@example.com",
                                              "displayName": "John Doe"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Authentication Failed",
                                    value = """
                                            {
                                              "timestamp": "2025-01-07T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Invalid email or password"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/login")
    public Mono<TokenResponse> login (
            @Parameter(description = "User login credentials", required = true)
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
    @Operation(
            summary = "Logout user and revoke refresh token",
            description = """
                    Invalidates the provided refresh token, effectively logging out the user.

                    **CQRS Command**: LogoutCommand → CommandBus → UserCommandHandler

                    **Security**: Requires valid JWT Bearer token in Authorization header.
                    **Action**: Marks refresh token as revoked in PostgreSQL.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Logout successful, refresh token revoked"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid refresh token",
                    content = @Content(mediaType = "application/json")
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(
            @Parameter(
                    description = "JWT Bearer token in format: Bearer <token>",
                    required = true,
                    example = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "Logout request with refresh token", required = true)
            @RequestBody LogoutRequest request
    ) {
        // Validate Bearer token is present (authentication handled by Spring Security)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new IllegalArgumentException("Authorization header with Bearer token is required"));
        }

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
    @Operation(
            summary = "Validate JWT access token",
            description = """
                    Validates the provided JWT access token and returns token claims.

                    **CQRS Query**: ValidateTokenQuery → QueryBus → QueryHandler

                    **Security**: Requires valid JWT Bearer token in Authorization header.
                    **Returns**: Token validity status and user claims (userId, email, roles).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token is valid, claims returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationResponse.class),
                            examples = @ExampleObject(
                                    name = "Valid Token",
                                    value = """
                                            {
                                              "isValid": true,
                                              "userId": "550e8400-e29b-41d4-a716-446655440000",
                                              "email": "user@example.com",
                                              "roles": ["USER"]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired token",
                    content = @Content(mediaType = "application/json")
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/validate")
    public Mono<ValidationResponse> validateToken(
            @Parameter(
                    description = "JWT Bearer token in format: Bearer <token>",
                    required = true,
                    example = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
            @RequestHeader("Authorization") String authHeader
    ) {
        // Validate Bearer token format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new IllegalArgumentException("Authorization header with Bearer token is required"));
        }

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
