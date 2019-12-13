package io.smallrye.mutiny.vertx;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class UniHelper {
    /**
     * Returns a {@link Uni} that, when subscribed, uses the provided {@code handler} to adapt a callback-based
     * asynchronous method.
     * <p>
     * For example:
     * 
     * <pre>
     * {
     *     &#64;code
     *     io.vertx.core.Vertx vertx = Vertx.vertx();
     *     Uni<String> blockingMethodResult = UniHelper
     *             .toMaybe(handler -> vertx.<String> executeBlocking(fut -> fut.complete(invokeBlocking()), handler));
     * }
     * </pre>
     * <p>
     * This is useful when using Mutiny without the Vert.x Mutiny API or your own asynchronous methods.
     *
     * @param handler the code executed when the returned {@link Uni} is subscribed.
     * @return the uni
     */
    public static <T> Uni<T> toUni(Consumer<Handler<AsyncResult<T>>> handler) {
        return AsyncResultUni.toUni(handler);
    }

    /**
     * Adapts an Vert.x {@code Handler<AsyncResult<T>>} to an {@link io.smallrye.mutiny.subscription.UniSubscriber}.
     * <p>
     * The returned observer can be subscribed to an {@link Uni#subscribe()}.
     *
     * @param handler the handler to adapt
     * @return the observer
     */
    public static <T> UniSubscriber<T> toSubscriber(Handler<AsyncResult<T>> handler) {
        AtomicBoolean terminated = new AtomicBoolean();
        return new UniSubscriber<T>() {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                // ignore it.
            }

            @Override
            public void onItem(T item) {
                if (terminated.compareAndSet(false, true)) {
                    handler.handle(Future.succeededFuture(item));
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (terminated.compareAndSet(false, true)) {
                    handler.handle(Future.failedFuture(failure));
                }
            }
        };
    }
}
