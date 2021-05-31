package io.smallrye.mutiny.vertx;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

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
        }
    }

    @Override
    public void subscribe(Subscriber<? super U> downstream) {
        Subscription sub = new Subscription() {
            @Override
            public void request(long req) {
                if (upstream.get() == this) {
                    source.fetch(req);
                }
            }

            @Override
            public void cancel() {
                release();
            }
        };
        if (!upstream.compareAndSet(null, sub)) {
            Subscriptions.fail(downstream, new IllegalStateException("This processor allows only a single Subscriber"));
            return;
        }

        source.pause();

        source.endHandler(v -> {
            release();
            downstream.onComplete();
        });
        source.exceptionHandler(err -> {
            release();
            downstream.onError(err);
        });
        source.handler(item -> downstream.onNext(transformation.apply(item)));

        downstream.onSubscribe(sub);
    }
}
