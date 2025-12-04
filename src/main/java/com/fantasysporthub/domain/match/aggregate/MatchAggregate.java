package com.fantasysporthub.domain.match.aggregate;

import com.fantasysporthub.cqrs.event.DomainEvent;
import com.fantasysporthub.domain.match.event.GoalScored;
import com.fantasysporthub.domain.match.event.MatchStarted;
import com.fantasysporthub.eventsourcing.core.EventMetadata;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
public class MatchAggregate {

    private UUID matchId;
    private MatchStatus status;
    private UUID homeTeamId;
    private UUID awayTeamId;
    private int homeScore;
    private int awayScore;
    private long version;

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    public static MatchAggregate create(
            UUID matchId,
            UUID homeTeamId,
            UUID awayTeamId,
            String sportId,
            String providerId,
            EventMetadata metadata
    ) {
        MatchAggregate match = new MatchAggregate();

        MatchStarted event = new MatchStarted(
                UUID.randomUUID(),
                matchId,
                Instant.now(),
                0L,
                homeTeamId,
                awayTeamId,
                sportId,
                providerId,
                metadata
        );

        match.applyEvent(event, true);
        return match;
    }

    /**
     * Record a goal.
     */
    public void recordGoal(
            UUID playerId,
            UUID teamId,
            int minute,
            boolean isPenalty,
            UUID assistPlayerId,
            String sportId,
            String providerId,
            EventMetadata metadata
    ) {
        // Business rule validation
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot record goal for match not in progress");
        }

        if (!teamId.equals(homeTeamId) && !teamId.equals(awayTeamId)) {
            throw new IllegalArgumentException("Team is not part of this match");
        }

        GoalScored event = new GoalScored(
                UUID.randomUUID(),
                matchId,
                Instant.now(),
                version + 1,
                playerId,
                teamId,
                minute,
                isPenalty,
                assistPlayerId,
                sportId,
                providerId,
                metadata
        );

        applyEvent(event, Boolean.TRUE);
    }

    private void applyEvent(DomainEvent event, boolean isNew) {
        switch (event) {
            case MatchStarted started -> {
                this.matchId = started.getAggregateId();
                this.homeTeamId = started.getHomeTeamId();
                this.awayTeamId = started.getAwayTeamId();
                this.status = MatchStatus.IN_PROGRESS;
                this.homeScore = 0;
                this.awayScore = 0;
                this.version = started.getVersion();
            }
            case GoalScored goal -> {
                if (goal.getTeamId().equals(homeTeamId)) {
                    this.homeScore++;
                } else {
                    this.awayScore++;
                }
                this.version = goal.getVersion();
            }
            default -> log.warn("Unknown event type: {}", event.getClass());
        }

        if (isNew) {
            uncommittedEvents.add(event);
        }
    }

    /**
     * Load aggregate from event history (Event Sourcing).
     */
    public static MatchAggregate loadFromHistory(List<DomainEvent> events) {
        MatchAggregate match = new MatchAggregate();
        events.forEach(event -> match.applyEvent(event, Boolean.FALSE));
        return match;
    }

    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    public enum MatchStatus {
        SCHEDULED,
        IN_PROGRESS,
        FINISHED,
        CANCELED
    }

}
