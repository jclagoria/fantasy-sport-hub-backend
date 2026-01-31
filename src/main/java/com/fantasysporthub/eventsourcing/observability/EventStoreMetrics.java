package com.fantasysporthub.eventsourcing.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Metrics for EventStore operations.
 */
@Component
public class EventStoreMetrics {

    private final Counter eventsAppended;
    private final Counter eventsRead;
    private final Counter eventsStreamed;
    private final Timer appendDuration;
    private final Timer readDuration;

    public EventStoreMetrics(MeterRegistry registry) {
        this.eventsAppended = Counter.builder("eventstore.events.appended")
                .description("Total events appended to streams")
                .tag("component", "eventstore")
                .register(registry);

        this.eventsRead = Counter.builder("eventstore.events.read")
                .description("Total events read from streams")
                .tag("component", "eventstore")
                .register(registry);

        this.eventsStreamed = Counter.builder("eventstore.events.streamed")
                .description("Total events streamed via subscriptions")
                .tag("component", "eventstore")
                .register(registry);

        this.appendDuration = Timer.builder("eventstore.append.duration")
                .description("Duration of event append operations")
                .tag("component", "eventstore")
                .register(registry);

        this.readDuration = Timer.builder("eventstore.read.duration")
                .description("Duration of event read operations")
                .tag("component", "eventstore")
                .register(registry);
    }

    public void recordEventAppended(String eventType) {
        eventsAppended.increment();
    }

    public void recordEventRead(String eventType) {
        eventsRead.increment();
    }

    public void recordEventStreamed(String eventType) {
        eventsStreamed.increment();
    }

}
