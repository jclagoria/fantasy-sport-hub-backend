package com.fantasysporthub.eventsourcing.exceptions;

/**
 * Exception thrown when attempting to deserialize an unregistered event type.
 */
public class UnknownEventTypeException extends RuntimeException {

    public UnknownEventTypeException(String message) {
        super(message);
    }

    public UnknownEventTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
