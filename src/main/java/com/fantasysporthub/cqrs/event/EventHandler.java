package com.fantasysporthub.cqrs.event;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface EventHandler<E extends DomainEvent> {

    Mono<Void> handle(E event);

}
