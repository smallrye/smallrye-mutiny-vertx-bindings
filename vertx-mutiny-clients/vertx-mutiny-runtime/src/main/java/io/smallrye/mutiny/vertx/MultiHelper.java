package io.smallrye.mutiny.vertx;

import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.vertx.core.streams.ReadStream;

public class MultiHelper {

    /**
     * Adapts an Mutiny {@link Multi<T>} to a Vert.x {@link io.vertx.core.streams.ReadStream<T>}. The returned
     * {@code ReadStream} will be subscribed to the {@link Multi<T>}.
     * <p>
     *
     * @param observable the observable to adapt
     * @return the adapted stream
     */
    public static <T> ReadStream<T> toReadStream(Multi<T> observable) {
        return ReadStreamSubscriber.asReadStream(observable, Function.identity());
    }

    /**
     * Like {@link #toMulti(ReadStream)} but with a {@code mapping} function
     */
    public static <T, U> Multi<U> toMulti(ReadStream<T> stream, Function<T, U> mapping) {
        return new MultiReadStream<>(stream, mapping);
    }

    /**
     * Adapts a Vert.x {@link ReadStream<T>} to an Mutiny {@link Multi<T>}. After
     * the stream is adapted to a Multi, the original stream handlers should not be used anymore
     * as they will be used by the Multi adapter.
     * <p>
     *
     * @param stream the stream to adapt
     * @return the adapted observable
     */
    public static <T> Multi<T> toMulti(ReadStream<T> stream) {
        return new MultiReadStream<>(stream, Function.identity());
    }

}
