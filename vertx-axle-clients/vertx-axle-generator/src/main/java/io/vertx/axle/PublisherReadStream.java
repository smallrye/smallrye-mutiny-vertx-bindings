package io.vertx.axle;

import io.smallrye.mutiny.helpers.Subscriptions;
import io.vertx.core.streams.ReadStream;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class PublisherReadStream<T, U> implements Publisher<U> {

    public static final long DEFAULT_MAX_BUFFER_SIZE = 256;

    private final ReadStream<T> stream;
    private final Function<T, U> f;
    private final AtomicReference<Subscription> current;

    public PublisherReadStream(ReadStream<T> stream, Function<T, U> f) {

        stream.pause();

        this.stream = stream;
        this.f = f;
        this.current = new AtomicReference<>();
    }

    private void release() {
        Subscription sub = current.get();
        if (sub != null) {
            if (current.compareAndSet(sub, null)) {
                try {
                    stream.exceptionHandler(null);
                    stream.endHandler(null);
                    stream.handler(null);
                } catch (Exception ignore) {
                } finally {
                    stream.resume();
                }
            }
        }
    }

    @Override
    public void subscribe(Subscriber<? super U> subscriber) {

        Subscription sub = new Subscription() {
            @Override
            public void request(long l) {
                if (current.get() == this) {
                    stream.fetch(l);
                }
            }

            @Override
            public void cancel() {
                release();
            }
        };
        if (!current.compareAndSet(null, sub)) {
            Subscriptions.fail(subscriber, new IllegalStateException("This processor allows only a single Subscriber"));
            return;
        }

        stream.pause();

        stream.endHandler(v -> {
            release();
            subscriber.onComplete();
        });
        stream.exceptionHandler(err -> {
            release();
            subscriber.onError(err);
        });
        stream.handler(item -> {
            subscriber.onNext(f.apply(item));
        });

        subscriber.onSubscribe(sub);
    }
}
