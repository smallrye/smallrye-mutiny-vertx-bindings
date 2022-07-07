package io.smallrye.mutiny.vertx;

import java.util.function.Consumer;

import io.vertx.core.Handler;

/**
 * A class being a {@link java.util.function.Consumer} and a {@link Handler} at the same time, and which delegate
 * {@link #hashCode()} and {@link #equals(Object)} to the consumer.
 *
 * @param <U>
 */
public class DelegatingConsumerHandler<U> implements Consumer<U>, Handler<U> {

    private final Consumer<U> consumer;

    public DelegatingConsumerHandler(Consumer<U> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void handle(U event) {
        accept(event);
    }

    @Override
    public void accept(U event) {
        this.consumer.accept(event);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DelegatingConsumerHandler<?> that = (DelegatingConsumerHandler<?>) o;
        return consumer.equals(that.consumer);
    }

    @Override
    public int hashCode() {
        return consumer.hashCode();
    }
}
