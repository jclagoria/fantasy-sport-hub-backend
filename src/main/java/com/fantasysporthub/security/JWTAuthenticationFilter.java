package com.fantasysporthub.security;

import com.fantasysporthub.domain.service.JWTService;
import com.fantasysporthub.domain.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;


/**
 * JWT authentication filter for Spring WebFlux.
 */
@Slf4j
@Component
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

        // Skip if no Authorization header or not Bearer token
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        var token = authHeader.substring(BEARER_PREFIX.length());

        return jwtService.validateToken(token)
                .flatMap(claims ->
                        tokenBlacklistService.isBlackListed(claims.tokenId())
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

                                    return chain.filter(exchange)
                                            .contextWrite(ReactiveSecurityContextHolder
                                                    .withAuthentication(authentication));
                                })
                )
                .onErrorResume(e -> {
                    log.debug("JWT validation failed: {}", e.getMessage());
                    return chain.filter(exchange);
                });
    }
}
