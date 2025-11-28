package com.fantasysporthub.eventsourcing.config;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadStreamOptions;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for EventStoreDB connection.
 */
@Component("eventstore")
public class EventStoreHealthIndicator implements HealthIndicator {

    private final EventStoreDBClient client;

    public EventStoreHealthIndicator(EventStoreDBClient client) {
        this.client = client;
    }

    @Override
    public Health health() {
        try {
            // Simple connectivity check
            client.readStream("$$$health-check", ReadStreamOptions.get()).get();
            return Health.up()
                    .withDetail("status", "connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
