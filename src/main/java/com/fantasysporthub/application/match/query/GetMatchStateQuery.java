package com.fantasysporthub.application.match.query;

import com.fantasysporthub.cqrs.query.Query;
import lombok.Value;

import java.util.UUID;

@Value
public class GetMatchStateQuery implements Query<MatchProjectionDTO> {

    UUID queryId;
    UUID matchId;

    @Override
    public UUID getQueryId() {
        return null;
    }

    @Override
    public Class<MatchProjectionDTO> getResultType() {
        return MatchProjectionDTO.class;
    }
}
