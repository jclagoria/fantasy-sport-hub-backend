package com.fantasysporthub.cqrs.query;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {

    Mono<R> handle(Q query);

}
