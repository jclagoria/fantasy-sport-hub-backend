package com.fantasysporthub.eventsourcing.testevents;

import com.fantasysporthub.eventsourcing.core.Event;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record TestEvent(
        UUID eventId,
        Instant timestamp,
        String eventType,
        int schemaVersion,
        String payload
) implements Event {

    public static TestEvent create(String payload) {
        return TestEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(Instant.now())
                .eventType("TestEvent")
                .schemaVersion(1)
                .payload(payload)
                .build();
    }

    public static TestEvent createWithType(String eventType, String payload) {
        return TestEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(Instant.now())
                .eventType(eventType)
                .schemaVersion(1)
                .payload(payload)
                .build();
    }
}