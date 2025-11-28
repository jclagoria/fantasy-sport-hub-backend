package com.fantasysporthub.eventsourcing.serialization;

import com.eventstore.dbclient.EventData;
import com.fantasysporthub.eventsourcing.core.Event;
import com.fantasysporthub.eventsourcing.exceptions.EventSerializationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Serializes domain events to EventStoreDB format.
 */
@Component
public class EventSerializer {

    private final ObjectMapper objectMapper;

    public EventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventData serialize(Event event) {
        try {
            var eventData = objectMapper.writeValueAsBytes(event);
            var metadata = objectMapper.writeValueAsBytes(event);

            return EventData.builderAsJson(event.eventType(), eventData)
                    .eventId(event.eventId())
                    .metadataAsBytes(metadata)
                    .build();

        } catch (Exception e) {
            throw new EventSerializationException(
                    "Failed to serialize event: " + event.eventType(), e
            );
        }
    }

}
