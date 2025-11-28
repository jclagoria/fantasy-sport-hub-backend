package com.fantasysporthub.eventsourcing.serialization;

import com.eventstore.dbclient.RecordedEvent;
import com.fantasysporthub.eventsourcing.core.Event;
import com.fantasysporthub.eventsourcing.exceptions.EventDeserializationException;
import com.fantasysporthub.eventsourcing.exceptions.UnknownEventTypeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deserializes events from EventStoreDB.
 */
@Component
public class EventDeserializer {

    private final ObjectMapper objectMapper;
    private final Map<String, Class<? extends Event>> eventTypeRegistry;

    public EventDeserializer(
            ObjectMapper objectMapper
    ) {
        this.eventTypeRegistry = new ConcurrentHashMap<>();
        this.objectMapper = objectMapper;
    }

    public void registryEventType(String eventType, Class<? extends Event> eventClass) {
        eventTypeRegistry.put(eventType, eventClass);
    }

    public Event deserialize(RecordedEvent recordedEvent) {
        var eventType = recordedEvent.getEventType();
        var eventClass = eventTypeRegistry.get(eventType);

        if (eventClass == null) {
            throw new UnknownEventTypeException(
                    "No registered class for event type: " + eventType
            );
        }

        try {
            return objectMapper.readValue(
                    recordedEvent.getEventData(),
                    eventClass);

        } catch (Exception e) {
            throw new EventDeserializationException(
                    "Failed to deserialize event type: " + eventType, e
            );
        }
    }

}
