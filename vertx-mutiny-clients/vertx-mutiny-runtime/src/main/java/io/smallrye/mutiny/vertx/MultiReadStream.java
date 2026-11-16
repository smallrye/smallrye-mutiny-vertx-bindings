package io.smallrye.mutiny.vertx;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.vertx.core.streams.ReadStream;

public class MultiReadStream<T, U> extends AbstractMulti<U> implements Multi<U> {

    private final ReadStream<T> source;
    private final Function<T, U> transformation;
    private final AtomicReference<Subscription> upstream;

    public MultiReadStream(ReadStream<T> source, Function<T, U> transformation) {
        source.pause();
        this.source = source;
        this.transformation = Infrastructure.decorate(transformation);
        this.upstream = new AtomicReference<>();
    }

    private void release() {
        Subscription sub = upstream.get();
        if (sub != null) {
            if (upstream.compareAndSet(sub, null)) {
                detach();
            }
        }
    }

    private void detach() {
        try {
            source.exceptionHandler(null);
            source.endHandler(null);
            source.handler(null);
        } catch (Exception ignore) {
        } finally {
            try {
                source.resume();
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void subscribe(Subscriber<? super U> downstream) {
        AtomicBoolean cancelled = new AtomicBoolean();
        Subscription sub = new Subscription() {
            @Override
            public void request(long req) {
                if (upstream.get() == this) {
                    source.fetch(req);
                }
            }

            @Override
            public void cancel() {
                cancelled.set(true);
                release();
            }
        };
        if (!upstream.compareAndSet(null, sub)) {
            Subscriptions.fail(downstream, new IllegalStateException("This processor allows only a single Subscriber"));
            return;
        }

        TerminalSignal terminal = new TerminalSignal();
        boolean registered = false;
        try {
            source.pause();
            source.endHandler(v -> terminal.signal(() -> {
                release();
                downstream.onComplete();
            }));
            source.exceptionHandler(err -> terminal.signal(() -> {
                release();
                downstream.onError(err);
            }));
            source.handler(item -> downstream.onNext(transformation.apply(item)));
            registered = true;
        } catch (Exception e) {
            // The stream may reject handlers, e.g., when it has already ended: signal it instead of hanging, unless the stream has already reported its failure.
            // Unlike after a completion or a cancellation, we remain subscribed, so further subscribers are rejected.
            terminal.signal(() -> {
                detach();
                downstream.onError(e);
            });
        }

        if (registered) {
            downstream.onSubscribe(sub);
        } else {
            downstream.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    // Nothing to fetch
                }

                @Override
                public void cancel() {
                    cancelled.set(true);
                }
            });
        }
        // A terminal signal reported before onSubscribe is not delivered if the subscriber has cancelled meanwhile
        if (!terminal.ready(!cancelled.get()) && !registered) {
            detach();
        }
    }

    /**
     * Ensures a single terminal signal, delivered after {@code onSubscribe}: a stream may report its end or a failure while the
     * handlers are being registered (e.g., an exception does not wait for the stream to be resumed).
     */
    private static class TerminalSignal {

        private boolean ready;
        private boolean done;
        private Runnable pending;

        void signal(Runnable action) {
            synchronized (this) {
                if (done) {
                    return;
                }
                done = true;
                if (!ready) {
                    pending = action;
                    return;
                }
            }
            action.run();
        }

        /**
         * @param deliver whether a pending terminal signal must be delivered
         * @return whether a pending terminal signal was delivered
         */
        boolean ready(boolean deliver) {
            Runnable action;
            synchronized (this) {
                ready = true;
                action = pending;
                pending = null;
            }
            if (action != null && deliver) {
                action.run();
                return true;
            }
            return false;
        }
    }
}
