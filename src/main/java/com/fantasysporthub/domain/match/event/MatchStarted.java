package com.fantasysporthub.domain.match.event;

import com.fantasysporthub.cqrs.event.DomainEvent;
import com.fantasysporthub.eventsourcing.core.EventMetadata;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
public class MatchStarted implements DomainEvent {

    UUID eventId;
    UUID aggregateId; //matchID
    String aggregateType;
    Instant timestamp;
    long version;

    // Match-specific data
    UUID homeTeamId;
    UUID awayTeamId;
    String sportId;
    String providerId;

    EventMetadata metadata;

    public MatchStarted(
            UUID eventId,
            UUID aggregateId,
            Instant timestamp,
            long version,
            UUID homeTeamId,
            UUID awayTeamId,
            String sportId,
            String providerId,
            EventMetadata metadata
    ) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.aggregateType = "Match";
        this.timestamp = timestamp;
        this.version = version;
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
        this.sportId = sportId;
        this.providerId = providerId;
        this.metadata = metadata != null ? metadata : EventMetadata.empty();
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
        return "MatchStarted";
    }

    @Override
    public int schemaVersion() {
        return 1;
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
}
