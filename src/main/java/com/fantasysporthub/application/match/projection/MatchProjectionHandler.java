package com.fantasysporthub.application.match.projection;

import com.fantasysporthub.application.match.query.MatchProjectionDTO;
import com.fantasysporthub.domain.match.event.GoalScored;
import com.fantasysporthub.domain.match.event.MatchStarted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    private final MongoTemplate mongoTemplate;

    /**
     * Handle MatchStarted event - create projection.
     */
    public Mono<Void> handleMatchStarted(MatchStarted event) {
        return Mono.fromRunnable(() -> {
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

            mongoTemplate.save(projection,  "match_projections");
            log.info("Match projection created: {}", event.getAggregateId());
        });
    }

    /**
     * Handle GoalScored event - update projection.
     */
    public Mono<Void> handleGoalSored(GoalScored event) {
        return Mono.fromRunnable(() ->  {
            log.info("Updating match projection for goal: {}", event.getAggregateId());

            var projection = mongoTemplate.findById(
                    event.getAggregateId().toString(),
                    MatchProjectionDTO.class,
                    "match_projections"
            );

            if (projection != null) {
                // Update score
                if (event.getTeamId().equals(projection.getHomeTeamId())) {
                    projection.setHomeScore(projection.getHomeScore() + 1);
                } else {
                    projection.setAwayScore(projection.getHomeScore() + 1);
                }

                // Add goal info
                projection.getGoals().add( new MatchProjectionDTO.GoalInfo(
                        event.getPlayerId(),
                        event.getTeamId(),
                        event.getMinute(),
                        event.isPenalty(),
                        event.getTimestamp()
                ));

                mongoTemplate.save(projection,  "match_projections");
                log.info("Match projection updated: {}", event.getAggregateId());
            } else {
                log.warn("Match projection not found: {}", event.getAggregateId());
            }
        });
    }
}