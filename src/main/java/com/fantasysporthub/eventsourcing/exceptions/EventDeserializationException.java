package com.fantasysporthub.eventsourcing.exceptions;

/**
 * Exception thrown when event deserialization fails.
 */
public class EventDeserializationException extends RuntimeException {

    public EventDeserializationException(String message) {
        super(message);
    }

    public EventDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
