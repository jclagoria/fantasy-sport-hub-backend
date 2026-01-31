package com.fantasysporthub.domain.user.event;

import com.fantasysporthub.cqrs.event.DomainEvent;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record LoginFailed(
        UUID eventId,
        String email,
        String reason,
        Instant occurredAt,
        String ipAddress,
        String userAgent
) implements DomainEvent {

    @Override
    public UUID getAggregateId() {
        return null;
    }

    @Override
    public String getAggregateType() {
        return "";
    }

    @Override
    public long getVersion() {
        return 0;
    }

    @Override
    public UUID eventId() {
        return null;
    }

    @Override
    public Instant timestamp() {
        return null;
    }

    @Override
    public String eventType() {
        return "";
    }

    @Override
    public int schemaVersion() {
        return 0;
    }
}
