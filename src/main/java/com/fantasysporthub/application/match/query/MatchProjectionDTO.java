package com.fantasysporthub.application.match.query;

import com.fantasysporthub.domain.match.aggregate.MatchAggregate;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Read model projection for match state.
 * Stored in MongoDB for optimized queries.
 */
@Data
public class MatchProjectionDTO {

    private String id;
    private String matchId;
    private UUID homeTeamId;
    private UUID awayTeamId;
    private int homeScore;
    private int awayScore;
    private MatchStatus matchStatus;
    private Instant startTime;
    private List<GoalInfo> goals = new ArrayList<>();

    @Data
    public static class GoalInfo {
        private UUID playerId;
        private UUID teamId;
        private int minute;
        private boolean isPenalty;
        private Instant timestamp;

        public GoalInfo(
                UUID playerId,
                UUID teamId,
                int minute,
                boolean isPenalty,
                Instant timestamp
        ) {
            this.playerId = playerId;
            this.teamId = teamId;
            this.minute = minute;
            this.isPenalty = isPenalty;
            this.timestamp = timestamp;
        }
    }

    public enum MatchStatus {
        SCHEDULED,
        IN_PROGRESS,
        FINISHED,
        CANCELED
    }

}
