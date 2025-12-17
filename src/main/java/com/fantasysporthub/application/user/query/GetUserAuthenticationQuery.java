package com.fantasysporthub.application.user.query;

import com.fantasysporthub.cqrs.query.Query;

import java.util.UUID;

/**
 * Query to retrieve user authentication data for JWT generation.
 */
public record GetUserAuthenticationQuery(UUID userId) implements Query<UserAuthenticationProjection>
{
    @Override
    public UUID getQueryId() {
        return null;
    }

    @Override
    public Class<UserAuthenticationProjection> getResultType() {
        return null;
    }
}
