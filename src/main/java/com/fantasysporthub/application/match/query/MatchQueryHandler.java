package com.fantasysporthub.application.match.query;

import com.fantasysporthub.cqrs.query.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Query handler for match read operations.
 * Reads from MongoDB projections (EXISTING infrastructure).
 */
@Component
@RequiredArgsConstructor
public class MatchQueryHandler implements QueryHandler<GetMatchStateQuery, MatchProjectionDTO> {

    private final MongoTemplate mongoTemplate;

    @Override
    public Mono<MatchProjectionDTO> handle(GetMatchStateQuery query) {
        return Mono.fromCallable(() ->
                mongoTemplate.findById(
                        query.getMatchId(),
                        MatchProjectionDTO.class,
                        "match_projections"
                )
        );
    }
}
