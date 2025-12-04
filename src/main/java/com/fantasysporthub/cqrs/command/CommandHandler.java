package com.fantasysporthub.cqrs.command;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface CommandHandler<C extends Command, R> {

    Mono<R> handle(C command);

}
