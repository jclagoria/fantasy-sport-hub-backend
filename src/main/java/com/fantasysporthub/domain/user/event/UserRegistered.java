package com.fantasysporthub.domain.user.event;

import com.fantasysporthub.cqrs.event.DomainEvent;
import lombok.Builder;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Builder
public record UserRegistered(
        UUID eventId,
        UUID userId,
        String email,
        String displayName,
        Set<String> roles,
        Instant occurredAt,
        String ipAddress,
        String userAgent
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
    public Instant timestamp() {
        return null;
    }

    @Override
    public String eventType() {
        return "UserRegistered";
    }

    @Override
    public int schemaVersion() {
        return  1;
    }
}
