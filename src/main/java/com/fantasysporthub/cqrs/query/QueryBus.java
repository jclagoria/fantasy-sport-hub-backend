package com.fantasysporthub.cqrs.query;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QueryBus {

    private final Map<Class<? extends Query<?>>, QueryHandler<?, ?>> handlers = new ConcurrentHashMap<>();

    public <Q extends Query<R>, R> void register (
            Class<Q> queryClass,
            QueryHandler<Q, R> handler
    ) {
        handlers.put(queryClass, handler);
    }

    @SuppressWarnings("unchecked")
    public <Q extends Query<R>, R> Mono<R> dispatch(Q query) {
        QueryHandler<Q, R> handler =
                (QueryHandler<Q, R>) handlers.get(query.getClass());

        if ( handler == null) {
            return Mono.error(new IllegalStateException(
                    "No handler registered for: " + query.getClass().getSimpleName()
            ));
        }

        return handler.handle(query);
    }
}
