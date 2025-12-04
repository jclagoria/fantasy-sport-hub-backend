package com.fantasysporthub.cqrs.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory event bus for CQRS domain events.
 * Publishes events to registered handlers (e.g., projections).
 */
@Slf4j
@Component
public class EventBus {

    private final Sinks.Many<DomainEvent> eventSink = Sinks.many().multicast().onBackpressureBuffer();

    private final Map<Class<? extends DomainEvent>, List<EventHandler<?>>> handlers =
            new ConcurrentHashMap<>();

    public Mono<Void> publish(DomainEvent event) {
        log.info("Publishing event: {} for aggregate: {}",
                event.getClass().getSimpleName(), event.getAggregateId());

        return Mono.fromRunnable(() -> {
            eventSink.tryEmitNext(event);
        });
    }

    public <E extends DomainEvent> void subscribe(
            Class<E> eventClass,
            EventHandler<E> handler
    ) {
        handlers.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(handler);

        log.info("Registered handler for event: {}", eventClass.getSimpleName());
    }

    public void start() {
        eventSink.asFlux().subscribe(this::processEvent);
    }

    @SuppressWarnings("unchecked")
    private <E extends DomainEvent> void processEvent(E event) {
        List<EventHandler<?>> eventHandlers = handlers.get(event.getClass());

        if (eventHandlers != null) {
            eventHandlers.forEach(handler -> {
                try {
                    ((EventHandler<E>) handler).handle(event).subscribe();
                } catch (Exception e) {
                    log.error("Error handling event: {}", event.getClass(), e);
                }
            });
        }
    }

}
