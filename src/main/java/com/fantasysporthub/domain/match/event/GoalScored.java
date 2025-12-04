package com.fantasysporthub.domain.match.event;

import com.fantasysporthub.cqrs.event.DomainEvent;
import com.fantasysporthub.eventsourcing.core.EventMetadata;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
public class GoalScored implements DomainEvent {

    UUID eventId;
    UUID aggregateId; //matchId
    String aggregateType;
    Instant timestamp;
    long version;

    // Goal-specific data
    UUID playerId;
    UUID teamId;
    int minute;
    boolean isPenalty;
    UUID assistPlayerId;
    String sportId;
    String providerId;

    EventMetadata metadata;

    public GoalScored (
        UUID eventId,
        UUID aggregateId,
        Instant timestamp,
        long version,
        UUID playerId,
        UUID teamId,
        int minute,
        boolean isPenalty,
        UUID assistPlayerId,
        String sportId,
        String providerId,
        EventMetadata metadata
    ) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.aggregateType = "Match";
        this.timestamp = timestamp;
        this.version = version;
        this.playerId = playerId;
        this.teamId = teamId;
        this.minute = minute;
        this.isPenalty = isPenalty;
        this.assistPlayerId = assistPlayerId;
        this.sportId = sportId;
        this.providerId = providerId;
        this.metadata = metadata != null ? metadata : EventMetadata.empty();
    }

    @Override
    public UUID getAggregateId() {
        return aggregateId;
    }

    @Override
    public String getAggregateType() {
        return aggregateType;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public UUID eventId() {
        return eventId;
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public String eventType() {
        return "GoalScored";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}
