package com.fantasysporthub.domain.match.aggregate;

import com.fantasysporthub.domain.match.event.MatchStarted;
import com.fantasysporthub.eventsourcing.core.EventMetadata;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchAggregateTest {

    @Test
    void shouldCreateMatchAndGenerateMatchStartedEvent() {
        // Given
        var matchId = UUID.randomUUID();
        var homeTeamId = UUID.randomUUID();
        var awayTeamId = UUID.randomUUID();

        // When
        var match = MatchAggregate.create(
                matchId,
                homeTeamId,
                awayTeamId,
                "FUTBOL",
                "api-football",
                EventMetadata.empty()
        );

        // Then
        assertThat(match.getMatchId()).isEqualTo(matchId);
        assertThat(match.getStatus()).isEqualTo(MatchAggregate.MatchStatus.IN_PROGRESS);
        assertThat(match.getHomeScore()).isEqualTo(0);
        assertThat(match.getAwayScore()).isEqualTo(0);
        assertThat(match.getUncommittedEvents()).hasSize(1);
        assertThat(match.getUncommittedEvents().getFirst()).isInstanceOf(MatchStarted.class);
    }

    @Test
    void shouldRecordGoalAndUpdateScore() {
        // Given
        var match = MatchAggregate.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "FUTBOL",
                "api-football",
                EventMetadata.empty()
        );
        match.markEventsAsCommitted();

        // When
        match.recordGoal(
                UUID.randomUUID(),
                match.getHomeTeamId(),
                23,
                false,
                null,
                "FUTBOL",
                "api-football",
                EventMetadata.empty()
        );

        // Then
        assertThat(match.getHomeScore()).isEqualTo(1);
        assertThat(match.getAwayScore()).isEqualTo(0);
        assertThat(match.getUncommittedEvents()).hasSize(1);
    }

    @Test
    void shouldLoadAggregateFromEventHistory() {
        // Given
        var matchId = UUID.randomUUID();
        var homeTeamId = UUID.randomUUID();
        var awayTeamId = UUID.randomUUID();

        var match1 = MatchAggregate.create(
                matchId,
                homeTeamId,
                awayTeamId,
                "FUTBOL",
                "api-football",
                EventMetadata.empty()
        );

        var events = match1.getUncommittedEvents();

        // When
        var match2 = MatchAggregate.loadFromHistory(events);

        // Then
        assertThat(match2.getMatchId()).isEqualTo(matchId);
        assertThat(match2.getHomeTeamId()).isEqualTo(homeTeamId);
        assertThat(match2.getStatus()).isEqualTo(MatchAggregate.MatchStatus.IN_PROGRESS);
    }

}