package com.fantasysporthub.application.user.query;

import com.fantasysporthub.cqrs.query.Query;

import java.util.UUID;

public record GetUserByEmailQuery(
        String email
) implements Query<UserProjectionDTO> {

    @Override
    public UUID getQueryId() {
        return null;
    }

    @Override
    public Class<UserProjectionDTO> getResultType() {
        return null;
    }
}
