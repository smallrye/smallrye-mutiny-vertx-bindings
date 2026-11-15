package io.smallrye.mutiny.vertx;

import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class AsyncResultUni<T> extends AbstractUni<T> implements Uni<T> {
    private final Consumer<Handler<AsyncResult<T>>> subscriptionConsumer;

    public static <T> Uni<T> toUni(Consumer<Handler<AsyncResult<T>>> subscriptionConsumer) {
        return new AsyncResultUni<>(subscriptionConsumer);
    }

    @SuppressWarnings("unchecked")
    public static <T> Uni<T> fromFuture(Supplier<? extends Future<? extends T>> futureSupplier) {
        return Infrastructure.onUniCreation(new AsyncResultUni<>(
                handler -> ((Future<T>) futureSupplier.get()).onComplete(ar -> handler.handle(unwrap(ar)))));
    }

    private static <T> AsyncResult<T> unwrap(AsyncResult<T> ar) {
        if (ar.failed() && (ar.cause() instanceof CompletionException) && (ar.cause().getCause() != null)) {
            return Future.failedFuture(ar.cause().getCause());
        }
        return ar;
    }

    public AsyncResultUni(Consumer<Handler<AsyncResult<T>>> subscriptionConsumer) {
        this.subscriptionConsumer = Infrastructure.decorate(subscriptionConsumer);
    }

    @Override
    public void subscribe(UniSubscriber<? super T> downstream) {
        AtomicBoolean terminated = new AtomicBoolean();
        downstream.onSubscribe(() -> terminated.set(true));

        if (!terminated.get()) {
            try {
                subscriptionConsumer.accept(ar -> {
                    if (!terminated.getAndSet(true)) {
                        if (ar.succeeded()) {
                            T val = ar.result();
                            downstream.onItem(val);
                        } else if (ar.failed()) {
                            downstream.onFailure(ar.cause());
                        }
                    }
                });
            } catch (Throwable e) {
                if (!terminated.getAndSet(true)) {
                    downstream.onFailure(e);
                } else {
                    // E.g., thrown by the downstream subscriber when the handler is called synchronously
                    Infrastructure.handleDroppedException(e);
                }
            }
        }
    }
}
