package com.fantasysporthub.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User login request")
public record LoginRequest(

        @Schema(
                description = "User email address",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(
                description = "User password",
                example = "SecurePassword123!",
                requiredMode = Schema.RequiredMode.REQUIRED,
                format = "password"
        )
        @NotBlank(message = "Password is required")
        String password
) {
}
