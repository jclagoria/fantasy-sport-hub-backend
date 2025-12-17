package com.fantasysporthub.infrastructure.config;

import com.fantasysporthub.security.JWTAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

/**
 * Spring Security WebFlux configuration.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTAuthenticationFilter jwtAuthentificationFilter;

    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Stateless session management (JWT-based)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                // Authorization rules
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints (Phase 1: Add /auth/register, /auth/login)
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Protected endpoints (Phase 0: Basic pattern)
                        .pathMatchers("/api/**").authenticated()

                        // Admin endpoints (Phase 1: Expand with role-based access)
                        .pathMatchers("/admin/**").hasRole("ADMIN")

                        // Default: require authentication
                        .anyExchange().authenticated()
                )

                // Add JWT authentication filter
                .addFilterAfter(jwtAuthentificationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

}
