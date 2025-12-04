package com.fantasysporthub.cqrs.command;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central command dispatcher using reactive patterns.
 * Routes commands to appropriate handlers.
 */
@Component
public class CommandBus {

    private final Map<Class<? extends Command>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();

    public <C extends Command, R> void register(
            Class<C> commandClass,
            CommandHandler<C, R> handler
    ) {
        handlers.put(commandClass, handler);
    }

    @SuppressWarnings("unchecked")
    public <C extends Command, R> Mono<R> dispatch(C command) {
        CommandHandler<C, R> handler = (CommandHandler<C, R>) handlers.get(command.getClass());

        if (handler == null) {
            return Mono.error(new IllegalStateException(
                    "No handler registered for: " + command.getClass().getSimpleName()
            ));
        }

        return handler.handle(command);
    }

}
