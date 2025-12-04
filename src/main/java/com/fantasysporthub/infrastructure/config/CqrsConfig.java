package com.fantasysporthub.infrastructure.config;

import com.fantasysporthub.application.match.command.MatchCommandHandler;
import com.fantasysporthub.application.match.projection.MatchProjectionHandler;
import com.fantasysporthub.application.match.query.GetMatchStateQuery;
import com.fantasysporthub.application.match.query.MatchQueryHandler;
import com.fantasysporthub.cqrs.command.CommandBus;
import com.fantasysporthub.cqrs.event.EventBus;
import com.fantasysporthub.cqrs.query.QueryBus;
import com.fantasysporthub.domain.match.command.RecordGoalCommand;
import com.fantasysporthub.domain.match.command.StartMatchCommand;
import com.fantasysporthub.domain.match.event.GoalScored;
import com.fantasysporthub.domain.match.event.MatchStarted;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * CQRS infrastructure configuration.
 * Registers all command handlers, query handlers, and event handlers.
 */
@Configuration
@RequiredArgsConstructor
public class CqrsConfig {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final EventBus eventBus;

    private final MatchCommandHandler matchCommandHandler;
    private final MatchQueryHandler matchQueryHandler;
    private final MatchProjectionHandler matchProjectionHandler;

    @EventListener(ApplicationReadyEvent.class)
    public void configureCqrs() {
        // Register command handlers
        commandBus.register(StartMatchCommand.class, matchCommandHandler::handleStartMatch);
        commandBus.register(RecordGoalCommand.class, matchCommandHandler::handleRecordGoal);

        // Register query handlers
        queryBus.register(GetMatchStateQuery.class, matchQueryHandler);

        // Register event handlers (projections)
        eventBus.subscribe(MatchStarted.class, matchProjectionHandler::handleMatchStarted);
        eventBus.subscribe(GoalScored.class, matchProjectionHandler::handleGoalSored);

        // Start the event bus
        eventBus.start();
    }
}
