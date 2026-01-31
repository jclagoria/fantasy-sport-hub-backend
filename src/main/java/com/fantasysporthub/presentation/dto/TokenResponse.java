package com.fantasysporthub.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "JWT token response after successful authentication")
public record TokenResponse(

        @Schema(
                description = "JWT access token (use in Authorization header as 'Bearer <token>')",
                example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJyb2xlcyI6WyJVU0VSIl0sImlhdCI6MTY0MTAwMDAwMCwiZXhwIjoxNjQxMDAzNjAwfQ.signature"
        )
        String accessToken,

        @Schema(
                description = "Refresh token UUID (use to obtain new access token)",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        String refreshToken,

        @Schema(
                description = "Access token expiration time in seconds",
                example = "3600"
        )
        int expiresIn,

        @Schema(
                description = "Token type (always 'Bearer')",
                example = "Bearer"
        )
        String tokenType,

        @Schema(
                description = "Authenticated user ID",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID userId,

        @Schema(
                description = "Authenticated user email",
                example = "user@example.com"
        )
        String email,

        @Schema(
                description = "Authenticated user display name",
                example = "John Doe"
        )
        String displayName
) {
}
