package com.fantasysporthub.eventsourcing.core;

import java.util.Map;
import java.util.UUID;

/**
 * Metadata associated with an event for tracing and correlation.
 */
public record EventMetadata(
        UUID correlationId,
        UUID causationId,
        String userId,
        Map<String, String> customMetadata
) {

    public static EventMetadata empty() {
        return new EventMetadata(null, null, null, Map.of());
    }

    public static EventMetadata withCorrelation(UUID correlationId) {
        return new EventMetadata(correlationId, null, null, Map.of());
    }

    public EventMetadata withCausation(UUID causationId) {
        return new EventMetadata(
                this.correlationId,
                causationId,
                this.userId,
                this.customMetadata
        );
    }

    public EventMetadata withUser(String userId) {
        return new EventMetadata(
                this.correlationId,
                this.causationId,
                userId,
                this.customMetadata
        );
    }



}

