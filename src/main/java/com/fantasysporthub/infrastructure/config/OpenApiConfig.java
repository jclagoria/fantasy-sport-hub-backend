package com.fantasysporthub.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 Configuration for Fantasy Sport Hub API Documentation.
 *
 * Provides interactive API documentation accessible at:
 * - Swagger UI: /swagger-ui.html or /webjars/swagger-ui/index.html
 * - OpenAPI JSON: /v3/api-docs
 * - OpenAPI YAML: /v3/api-docs.yaml
 *
 * This configuration defines:
 * - API metadata (title, version, description, contact)
 * - JWT Bearer authentication scheme
 * - Server endpoints for different environments
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Fantasy Sport Hub Backend}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .components(securitySchemes())
                .addSecurityItem(securityRequirement());
    }

    /**
     * API Information metadata.
     */
    private Info apiInfo() {
        return new Info()
                .title("Fantasy Sport Hub API")
                .version("1.0.0")
                .description("""
                        **Fantasy Sport Hub Backend API** - Reactive Spring Boot application implementing event-sourced CQRS architecture.

                        ## Architecture
                        - **CQRS Pattern**: Separate Command (write) and Query (read) operations
                        - **Event Sourcing**: Complete audit trail via EventStoreDB
                        - **Reactive Stack**: Spring WebFlux with Project Reactor
                        - **Hybrid Data Model**: PostgreSQL (write model), EventStoreDB (event store), MongoDB (future read optimizations)

                        ## Authentication
                        - JWT-based authentication with RSA key pairs
                        - Access tokens (short-lived, 1 hour)
                        - Refresh tokens (long-lived, 7 days, stored in PostgreSQL)
                        - Password hashing with Argon2id

                        ## Public Endpoints
                        - `POST /auth/register` - User registration
                        - `POST /auth/login` - User authentication

                        ## Protected Endpoints
                        All other endpoints require JWT Bearer token in Authorization header.
                        """)
                .contact(new Contact()
                        .name("Fantasy Sport Hub Team")
                        .email("support@fantasysporthub.com")
                        .url("https://fantasysporthub.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * Server list for different environments.
     */
    private List<Server> serverList() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local Development Server"),
                new Server()
                        .url("https://api-dev.fantasysporthub.com")
                        .description("Development Environment"),
                new Server()
                        .url("https://api-staging.fantasysporthub.com")
                        .description("Staging Environment"),
                new Server()
                        .url("https://api.fantasysporthub.com")
                        .description("Production Environment")
        );
    }

    /**
     * Security schemes configuration (JWT Bearer).
     */
    private Components securitySchemes() {
        return new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Bearer token authentication. Obtain token via `/auth/login` endpoint."));
    }

    /**
     * Global security requirement (apply JWT to all endpoints by default).
     */
    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("bearerAuth");
    }
}
