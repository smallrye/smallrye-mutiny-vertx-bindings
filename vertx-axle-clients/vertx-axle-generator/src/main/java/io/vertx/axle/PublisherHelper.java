package io.vertx.axle;

import io.smallrye.mutiny.Multi;
import io.vertx.core.streams.ReadStream;
import org.reactivestreams.Publisher;

import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class PublisherHelper {

    private PublisherHelper() {
        // Avoid direct instantiation.
    }

    /**
     * Like {@link #toPublisher(ReadStream)} but with a {@code mapping} function
     */
    public static <T, U> Publisher<U> toPublisher(ReadStream<T> stream, Function<T, U> mapping) {
        return Multi.createFrom().publisher(new PublisherReadStream<>(stream, mapping));
    }

    /**
     * Adapts a Vert.x {@link ReadStream<T>} to a Reactive Streams {@link Publisher<T>}. After
     * the stream is adapted to a publisher, the original stream handlers should not be used anymore
     * as they will be used by the publisher adapter.
     * <p>
     *
     * @param stream the stream to adapt
     * @return the adapted observable
     */
    public static <T> Publisher<T> toPublisher(ReadStream<T> stream) {
        return Multi.createFrom().publisher(new PublisherReadStream<>(stream, Function.identity()));
    }
}
