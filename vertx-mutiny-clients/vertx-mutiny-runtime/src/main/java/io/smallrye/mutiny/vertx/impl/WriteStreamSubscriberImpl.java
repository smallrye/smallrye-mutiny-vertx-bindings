package io.smallrye.mutiny.vertx.impl;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.vertx.WriteStreamSubscriber;
import io.vertx.core.streams.WriteStream;

public class WriteStreamSubscriberImpl<I, O> implements WriteStreamSubscriber<I> {

    private static final int BATCH_SIZE = 16;

    private final WriteStream<O> stream;
    private final Function<I, O> mapping;

    private AtomicReference<Subscription> upstream = new AtomicReference<>();
    private AtomicBoolean done = new AtomicBoolean();
    private int outstanding;

    private Consumer<? super Throwable> onFailure;
    private Runnable onCompletion;
    private Consumer<? super Throwable> onStreamFailure;

    public WriteStreamSubscriberImpl(WriteStream<O> stream, Function<I, O> mapping) {
        this.stream = ParameterValidation.nonNull(stream, "writeStream");
        this.mapping = ParameterValidation.nonNull(mapping, "mapping");
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        ParameterValidation.nonNullNpe(subscription, "upstream");
        if (upstream.compareAndSet(null, subscription)) {
            stream.exceptionHandler(t -> {
                if (done.getAndSet(true)) {
                    return;
                }
                cancel();
                Consumer<? super Throwable> onFailureCallback;
                synchronized (this) {
                    onFailureCallback = this.onStreamFailure;
                }
                if (onFailureCallback != null) {
                    try {
                        onFailureCallback.accept(t);
                    } catch (Exception ignored) {
                        // ignore it
                    }
                }
            });
            stream.drainHandler(v -> requestMore());
            requestMore();
        } else {
            subscription.cancel();
        }
    }

    private void cancel() {
        Subscriptions.cancel(upstream);
    }

    @Override
    public void onNext(I item) {
        if (done.get()) {
            return;
        }

        if (item == null) {
            Throwable throwable = new NullPointerException("onNext called with null");
            try {
                cancel();
            } catch (Throwable t) {
                throwable = new CompositeException(throwable, t);
            }
            onError(throwable);
            return;
        }

        try {
            stream.write(mapping.apply(item));
            synchronized (this) {
                outstanding--;
            }
        } catch (Throwable t) {
            Throwable throwable;
            try {
                cancel();
                throwable = t;
            } catch (Throwable t1) {
                throwable = new CompositeException(t, t1);
            }
            onError(throwable);
            return;
        }

        if (!stream.writeQueueFull()) {
            requestMore();
        }
    }

    @Override
    public void onError(Throwable failure) {
        if (done.getAndSet(true)) {
            return;
        }

        ParameterValidation.nonNullNpe(failure, "failure");

        Consumer<? super Throwable> c;
        synchronized (this) {
            c = onFailure;
        }
        try {
            if (c != null) {
                c.accept(failure);
            }
        } catch (Throwable ignored) {
            // ignore it.
        }
    }

    @Override
    public void onComplete() {
        if (done.getAndSet(true)) {
            return;
        }

        Runnable completionCallback;
        synchronized (this) {
            completionCallback = onCompletion;
        }
        try {
            stream.end();
            if (completionCallback != null) {
                completionCallback.run();
            }
        } catch (Throwable ignored) {
            // ignored
        }
    }

    private void requestMore() {
        Subscription s = upstream.get();
        if (s == null) {
            return;
        }

        synchronized (this) {
            if (done.get() || outstanding > 0) {
                return;
            }
            outstanding = BATCH_SIZE;
        }
        s.request(BATCH_SIZE);
    }

    @Override
    public synchronized WriteStreamSubscriber<I> onFailure(Consumer<? super Throwable> handler) {
        this.onFailure = handler;
        return this;
    }

    @Override
    public synchronized WriteStreamSubscriber<I> onComplete(Runnable handler) {
        this.onCompletion = handler;
        return this;
    }

    @Override
    public synchronized WriteStreamSubscriber<I> onWriteStreamError(Consumer<? super Throwable> handler) {
        this.onStreamFailure = handler;
        return this;
    }
}
