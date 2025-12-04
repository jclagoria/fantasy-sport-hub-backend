package com.fantasysporthub.cqrs.query;

import java.util.UUID;

/**
 * Base interface for all queries in the system.
 * Queries retrieve data without side effects.
 */
public interface Query<R> {

    UUID getQueryId();
    Class<R> getResultType();

}
