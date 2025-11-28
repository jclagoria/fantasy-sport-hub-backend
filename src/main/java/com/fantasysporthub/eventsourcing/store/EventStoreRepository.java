package com.fantasysporthub.eventsourcing.store;

import com.eventstore.dbclient.*;
import com.fantasysporthub.eventsourcing.core.Event;
import com.fantasysporthub.eventsourcing.core.StreamName;
import com.fantasysporthub.eventsourcing.observability.EventStoreMetrics;
import com.fantasysporthub.eventsourcing.serialization.EventDeserializer;
import com.fantasysporthub.eventsourcing.serialization.EventSerializer;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive repository for EventStoreDB operations.
 * Provides event persistence and retrieval with optimistic concurrency.
 */
@Repository
public class EventStoreRepository {

    private final EventStoreDBClient client;
    private final EventSerializer serializer;
    private final EventDeserializer deserializer;
    private final EventStoreMetrics metrics;
    private final Tracer tracer;

    public EventStoreRepository(
            EventStoreDBClient client,
            EventSerializer serializer,
            EventDeserializer deserializer,
            EventStoreMetrics metrics,
            Tracer tracer
    ) {
        this.client = client;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.metrics = metrics;
        this.tracer = tracer;
    }

    /**
     * Append single event to stream with optimistic concurrency.
     */
    public Mono<EventStoreWriteResult> appendEvent(
            StreamName streamName,
            Event event,
            ExpectedRevision expectedRevision
    ) {
        var span = tracer.spanBuilder("eventstore.append")
                .setAttribute("stream", streamName.value())
                .setAttribute("eventType", event.eventType())
                .startSpan();

        return Mono.fromCallable(() -> {
            var eventData = serializer.serialize(event);
            var options = AppendToStreamOptions.get()
                    .expectedRevision(expectedRevision);
            var result = client.appendToStream(
                    streamName.value(),
                    options,
                    eventData
            ).get();

            metrics.recordEventAppended(event.eventType());
            span.end();

            return EventStoreWriteResult.from(result);
        });
    }

    /**
     * Append multiple events to stream atomically.
     */
    public Mono<EventStoreWriteResult> appendEvents(
            StreamName streamName,
            List<Event> events,
            ExpectedRevision expectedRevision
    ) {
        var span = tracer.spanBuilder("eventstore.append.batch")
                .setAttribute("stream", streamName.value())
                .setAttribute("eventCount", events.size())
                .startSpan();

        return Mono.fromCallable(() -> {
            var eventDataList = events.stream()
                    .map(serializer::serialize)
                    .toList();

            var options = AppendToStreamOptions.get()
                    .expectedRevision(expectedRevision);
            var result = client.appendToStream(
                    streamName.value(),
                    options,
                    eventDataList.iterator()
            ).get();

            events.forEach(e -> metrics.recordEventAppended(e.eventType()));
            span.end();

            return EventStoreWriteResult.from(result);
        });
    }

    /**
     * Read all events from stream.
     */
    public Flux<Event> readStream(StreamName streamName) {
        return Flux.create(sink -> {
            try {
                var readResult = client.readStream(
                        streamName.value(),
                        ReadStreamOptions.get()
                                .forwards()
                                .fromStart()
                ).get();

                readResult.getEvents().forEach(resolvedEvent -> {
                    try {
                        var event = deserializer.deserialize(
                                resolvedEvent.getEvent()
                        );
                        sink.next(event);
                        metrics.recordEventRead(event.eventType());
                    } catch (Exception e) {
                        sink.error(e);
                    }
                });

                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    /**
     * Read events from specific revision.
     */
    public Flux<Event> readStreamFrom(StreamName streamName, long fromRevision) {
        return Flux.create(sink -> {
            try {
                var readResult = client.readStream(
                        streamName.value(),
                        ReadStreamOptions.get()
                                .forwards()
                                .fromRevision(fromRevision)
                ).get();

                readResult.getEvents().forEach(resolvedEvent -> {
                    try {
                        var event = deserializer.deserialize(
                                resolvedEvent.getEvent()
                        );
                        sink.next(event);
                    } catch (Exception e) {
                        sink.error(e);
                    }
                });

                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    /**
     * Subscribe to stream with catch-up.
     */
    public Flux<Event> subscribeToStream(StreamName streamName) {
        return Flux.create(sink -> {
            try {
                var subscription = client.subscribeToStream(
                        streamName.value(),
                        new SubscriptionListener() {
                            @Override
                            public void onEvent(
                                    Subscription subscription,
                                    ResolvedEvent resolvedEvent
                            ) {
                                try {
                                    var event = deserializer.deserialize(
                                            resolvedEvent.getEvent()
                                    );
                                    sink.next(event);
                                    metrics.recordEventStreamed(event.eventType());
                                } catch (Exception e) {
                                    sink.error(e);
                                }
                            }

                            @Override
                            public void onCancelled(
                                    Subscription subscription,
                                    Throwable exception
                            ) {
                                sink.complete();
                            }
                        }
                ).get();

                sink.onDispose(subscription::stop);
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    /**
     * Check if stream exists.
     */
    public Mono<Boolean> streamExists(StreamName streamName) {
        return Mono.fromCallable(() -> {
            try {
                client.readStream(
                        streamName.value(),
                        ReadStreamOptions.get().forwards().fromStart().maxCount(1)
                ).get();
                return true;
            } catch (StreamNotFoundException e) {
                return false;
            }
        });
    }
}
