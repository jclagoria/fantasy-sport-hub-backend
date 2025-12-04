package com.fantasysporthub.eventsourcing.testevents;

import com.fantasysporthub.eventsourcing.core.Event;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PlayerCreatedEvent(
        UUID eventId,
        Instant timestamp,
        String eventType,
        int schemaVersion,
        String playerId,
        String playerName,
        String team
) implements Event {

    public static PlayerCreatedEvent create(String playerId, String playerName, String team) {
        return PlayerCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(Instant.now())
                .eventType("PlayerCreated")
                .schemaVersion(1)
                .playerId(playerId)
                .playerName(playerName)
                .team(team)
                .build();
    }
}