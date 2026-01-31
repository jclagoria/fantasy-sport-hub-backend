package com.fantasysporthub.application.match.projection;

import com.fantasysporthub.application.match.query.MatchProjectionDTO;
import com.fantasysporthub.domain.match.event.GoalScored;
import com.fantasysporthub.domain.match.event.MatchStarted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Event handler that maintains match projections in MongoDB.
 * Updates read model when domain events occur.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchProjectionHandler {

    private final ReactiveMongoTemplate mongoTemplate;

    /**
     * Handle MatchStarted event - create projection.
     */
    public Mono<Void> handleMatchStarted(MatchStarted event) {
        log.info("Creating match projection for: {}", event.getAggregateId());

        var projection = new MatchProjectionDTO();
        projection.setId(event.getAggregateId().toString());
        projection.setMatchId(event.getAggregateId().toString());
        projection.setHomeTeamId(event.getHomeTeamId());
        projection.setAwayTeamId(event.getAwayTeamId());
        projection.setMatchStatus(MatchProjectionDTO.MatchStatus.IN_PROGRESS);
        projection.setStartTime(event.getTimestamp());
        projection.setHomeScore(0);
        projection.setAwayScore(0);

        return mongoTemplate.save(projection, "match_projections")
                .doOnSuccess(saved -> log.info("Match projection created: {}", event.getAggregateId()))
                .then();
    }

    /**
     * Handle GoalScored event - update projection.
     */
    public Mono<Void> handleGoalSored(GoalScored event) {
        log.info("Updating match projection for goal: {}", event.getAggregateId());

        return mongoTemplate.findById(
                        event.getAggregateId().toString(),
                        MatchProjectionDTO.class,
                        "match_projections"
                )
                .flatMap(projection -> {
                    // Update score
                    if (event.getTeamId().equals(projection.getHomeTeamId())) {
                        projection.setHomeScore(projection.getHomeScore() + 1);
                    } else {
                        projection.setAwayScore(projection.getAwayScore() + 1);
                    }

                    // Add goal info
                    projection.getGoals().add(new MatchProjectionDTO.GoalInfo(
                            event.getPlayerId(),
                            event.getTeamId(),
                            event.getMinute(),
                            event.isPenalty(),
                            event.getTimestamp()
                    ));

                    return mongoTemplate.save(projection, "match_projections")
                            .doOnSuccess(saved -> log.info("Match projection updated: {}", event.getAggregateId()));
                })
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.warn("Match projection not found: {}", event.getAggregateId())))
                .then();
    }
}