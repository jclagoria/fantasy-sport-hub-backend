package com.fantasysporthub.cqrs.event;

import com.fantasysporthub.eventsourcing.core.Event;

import java.util.UUID;

/**
 * CQRS domain event that extends Event Sourcing Event.
 * Adds CQRS-specific metadata (aggregateId, aggregateType, version).
 */
public interface DomainEvent extends Event {

    UUID getAggregateId();
    String getAggregateType();
    long getVersion();

}
