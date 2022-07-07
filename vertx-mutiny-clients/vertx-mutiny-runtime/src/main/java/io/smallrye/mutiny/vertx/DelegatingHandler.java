package io.smallrye.mutiny.vertx;

import java.util.function.Function;

import io.vertx.core.Handler;

/**
 * An implementation of {@link Handler} and which delegates {@link #hashCode()} and {@link #equals(Object)} to
 * the passed handler.
 *
 * @param <U>
 */
public class DelegatingHandler<U, V> implements Handler<U> {

    private final Handler<V> handler;
    private final Function<U, V> mapper;

    public DelegatingHandler(Handler<V> handler, Function<U, V> mapper) {
        this.handler = handler;
        this.mapper = mapper;
    }

    @Override
    public void handle(U event) {
        handler.handle(mapper.apply(event));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DelegatingHandler<?, ?> that = (DelegatingHandler<?, ?>) o;
        return handler.equals(that.handler);
    }

    @Override
    public int hashCode() {
        return handler.hashCode();
    }
}
