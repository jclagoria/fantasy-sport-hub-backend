package com.fantasysporthub.presentation.dto;

import java.util.UUID;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        int expiresIn,
        String tokenType,
        UUID userId,
        String email,
        String displayName
) {
}
