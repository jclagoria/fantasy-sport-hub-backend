package com.fantasysporthub.security;

import com.fantasysporthub.domain.service.JWTService;
import com.fantasysporthub.domain.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;


/**
 * JWT authentication filter for Spring WebFlux.
 */
@Slf4j
@RequiredArgsConstructor
public class JWTAuthenticationFilter implements WebFilter {

    private final JWTService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        // Skip if no Authorization header
        if (authHeader == null || authHeader.isBlank()) {
            return chain.filter(exchange);
        }

        // Extract token: support both "Bearer <token>" and raw "<token>" formats
        var token = authHeader.startsWith(BEARER_PREFIX)
                ? authHeader.substring(BEARER_PREFIX.length())
                : authHeader;

        return jwtService.validateToken(token)
                .flatMap(claims ->
                        // Check blacklist with fail-open: if Redis is unavailable, assume token is valid
                        tokenBlacklistService.isBlackListed(claims.tokenId())
                                .onErrorResume(redisError -> {
                                    log.warn("Redis blacklist check failed (fail-open): {}", redisError.getMessage());
                                    return Mono.just(Boolean.FALSE); // Fail-open: assume not blacklisted
                                })
                                .flatMap(isBlackListed -> {
                                    if (isBlackListed) {
                                        log.warn("Rejected blacklisted token: {}", claims.tokenId());
                                        return chain.filter(exchange);
                                    }

                                    var authorities = claims.roles().stream()
                                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                            .toList();

                                    var authentication = new UsernamePasswordAuthenticationToken(
                                            claims.userId(),
                                            null,
                                            authorities
                                    );

                                    log.debug("JWT authentication successful for user: {}", claims.email());

                                    return chain.filter(exchange)
                                            .contextWrite(ReactiveSecurityContextHolder
                                                    .withAuthentication(authentication));
                                })
                )
                .onErrorResume(e -> {
                    log.warn("JWT validation failed: {}", e.getMessage());
                    return chain.filter(exchange);
                });
    }
}
