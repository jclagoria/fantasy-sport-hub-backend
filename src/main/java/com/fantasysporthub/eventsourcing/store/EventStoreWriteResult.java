package com.fantasysporthub.eventsourcing.store;

import com.eventstore.dbclient.WriteResult;

/**
 * Result of event append operation.
 */
public record EventStoreWriteResult(
        long nextExpectedRevision,
        String streamName
) {

    public static EventStoreWriteResult from (WriteResult writeResult) {
        return new EventStoreWriteResult(
                writeResult.getNextExpectedRevision().toRawLong(),
                ""
        );
    }

}
