package com.fantasysporthub.eventsourcing.core;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events.
 * All events are immutable and represent historical facts.
 */
public interface Event {

    UUID eventId();

    Instant timestamp();

    String eventType();

    int schemaVersion();

    default EventMetadata metadata() {
        return EventMetadata.empty();
    }

}
