package com.fantasysporthub.domain.user.event;

import com.fantasysporthub.cqrs.event.DomainEvent;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record LoginSuccessful(
        UUID eventId,
        UUID userId,
        String email,
        Instant occurredAt,
        String ipAddress,
        String userAgent,
        String deviceFingerprint
) implements DomainEvent {

    @Override
    public UUID getAggregateId() {
        return userId;
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
        return "LoginSuccessful";
    }

    @Override
    public int schemaVersion() {
        return 0;
    }
}
