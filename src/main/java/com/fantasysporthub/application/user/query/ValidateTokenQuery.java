package com.fantasysporthub.application.user.query;

import com.fantasysporthub.cqrs.query.Query;
import com.fantasysporthub.security.JWTClaims;

import java.util.UUID;

public record ValidateTokenQuery(
        String token
) implements Query<JWTClaims> {

    @Override
    public UUID getQueryId() {
        return null;
    }

    @Override
    public Class<JWTClaims> getResultType() {
        return null;
    }
}
