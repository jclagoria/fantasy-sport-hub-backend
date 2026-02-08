package com.fantasysporthub.infrastructure.config;

import com.fantasysporthub.domain.service.JWTService;
import com.fantasysporthub.domain.service.TokenBlacklistService;
import com.fantasysporthub.security.JWTAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

/**
 * Spring Security WebFlux configuration.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Stateless session management (JWT-based)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                // Custom authentication entry point to return 401 JSON instead of browser popup
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                )

                // Authorization rules
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints (no authentication required)
                        .pathMatchers("/auth/register", "/auth/login").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()

                        // OpenAPI / Swagger UI endpoints
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/webjars/**").permitAll()

                        // Auth endpoints that require valid JWT token
                        // /auth/validate - validates the token passed in Authorization header
                        // /auth/logout - requires authenticated user to revoke their refresh token
                        .pathMatchers("/auth/validate", "/auth/logout").authenticated()

                        // Protected API endpoints
                        .pathMatchers("/api/**").authenticated()

                        // Admin endpoints
                        .pathMatchers("/admin/**").hasRole("ADMIN")

                        // Default: require authentication
                        .anyExchange().authenticated()
                )

                // Add JWT authentication filter ONLY in the security chain (not as global WebFilter)
                .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    /**
     * Create JWTAuthenticationFilter bean explicitly to avoid double registration.
     * NOT annotated with @Component to prevent Spring from also registering it
     * as a global WebFilter (which would cause the filter to execute twice).
     */
    @Bean
    public JWTAuthenticationFilter jwtAuthenticationFilter() {
        return new JWTAuthenticationFilter(jwtService, tokenBlacklistService);
    }

    /**
     * Custom authentication entry point that returns 401 JSON response
     * instead of triggering browser's HTTP Basic Auth popup.
     */
    private ServerAuthenticationEntryPoint unauthorizedEntryPoint() {
        return (exchange, ex) -> {
            var response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("Content-Type", "application/json");
            response.getHeaders().add("WWW-Authenticate", "Bearer realm=\"api\"");

            var body = "{\"error\":\"Unauthorized\",\"message\":\"Valid JWT token required\",\"status\":401}";
            var buffer = response.bufferFactory().wrap(body.getBytes());
            return response.writeWith(Mono.just(buffer));
        };
    }

}
