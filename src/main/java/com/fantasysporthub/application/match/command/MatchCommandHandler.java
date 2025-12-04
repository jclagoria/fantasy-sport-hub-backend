package com.fantasysporthub.application.match.command;

import com.eventstore.dbclient.ExpectedRevision;
import com.fantasysporthub.cqrs.event.DomainEvent;
import com.fantasysporthub.cqrs.event.EventBus;
import com.fantasysporthub.domain.match.aggregate.MatchAggregate;
import com.fantasysporthub.domain.match.command.RecordGoalCommand;
import com.fantasysporthub.domain.match.command.StartMatchCommand;
import com.fantasysporthub.eventsourcing.core.EventMetadata;
import com.fantasysporthub.eventsourcing.core.StreamName;
import com.fantasysporthub.eventsourcing.store.EventStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Command handler for match operations.
 * Integrates CQRS with Event Sourcing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchCommandHandler {

    private final EventStoreRepository eventStoreRepository;
    private final EventBus eventBus;

    public Mono<UUID> handleStartMatch(StartMatchCommand command) {
        return Mono.fromCallable(() -> {
            log.info("Handling StartMatch command for match: {}", command.getMatchId());

            var metadata = EventMetadata
                    .withCorrelation(command.getCommandId())
                    .withUser(command.getUserId().toString());

            MatchAggregate match = MatchAggregate.create(
                    command.getMatchId(),
                    command.getHomeTeamId(),
                    command.getAwayTeamId(),
                    command.getSportId(),
                    command.getProviderId(),
                    metadata
            );

            var streamName = StreamName.match(command.getMatchId().toString());
            match.getUncommittedEvents().forEach(event -> {
                eventStoreRepository.appendEvent(
                        streamName,
                        event,
                        ExpectedRevision.noStream()
                ).block();

                // Publish to CQRS EventBus for projections
                eventBus.publish(event);
            });

            match.markEventsAsCommitted();

            log.info("Match started successfully: {}", command.getMatchId());
            return match.getMatchId();
        });
    }

    /**
     * Handle RecordGoal command.
     */
    public Mono<UUID> handleRecordGoal(RecordGoalCommand command) {
        return Mono.fromCallable(() -> {
            log.info("Handling RecordGoal command for match: {}", command.getMatchId());

            var streamName = StreamName.match(command.getMatchId().toString());
            var events = eventStoreRepository.readStream(streamName)
                    .collectList()
                    .block();

            if (events == null || events.isEmpty()) {
                throw new IllegalStateException("Match not found: " + command.getMatchId());
            }

            var domainEvents = events.stream()
                    .map(e -> (DomainEvent)e)
                    .toList();

            MatchAggregate match = MatchAggregate.loadFromHistory(domainEvents);

            var metadata = EventMetadata
                    .withCorrelation(command.getCommandId())
                    .withUser(command.getUserId().toString());

            match.recordGoal(
                    command.getPlayerId(),
                    command.getTeamId(),
                    command.getMinute(),
                    command.isPenalty(),
                    command.getAssistPlayerId(),
                    command.getSportId(),
                    command.getProviderId(),
                    metadata
            );

            // Persist new events
            match.getUncommittedEvents().forEach(event -> {
                eventStoreRepository.appendEvent(
                                streamName,
                                event,
                                ExpectedRevision
                                        .expectedRevision(match.getVersion() - 1))
                        .block();
            });

            match.markEventsAsCommitted();

            log.info("Goal recorded successfully for match: {}", command.getMatchId());
            return command.getMatchId();
        });
    }

}
