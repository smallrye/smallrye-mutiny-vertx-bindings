package io.smallrye.mutiny.vertx.core;

import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Expectation;

/**
 * Helper methods to turn Vert.x {@link Expectation} that work on {@link io.vertx.core.Future} into {@link Uni} that
 * can be used in a pipeline using the {@link Uni#plug(Function)} operator, as in:
 *
 * <pre>{@code
 * vertx.createHttpClient()
 *         .request(HttpMethod.GET, port, "localhost", "/")
 *         .chain(HttpClientRequest::send)
 *         .plug(expectation(HttpClientResponse::getDelegate, status(200).and(contentType("text/plain"))))
 *         .onItem().transformToUni(HttpClientResponse::body)
 * }</pre>
 */
public interface Expectations {

    /**
     * Yields a function to turn an {@link Expectation} into a {@link Uni}.
     *
     * @param expectation the expectation
     * @return the mapping function
     * @param <T> the element type
     */
    static <T> Function<Uni<T>, Uni<T>> expectation(Expectation<? super T> expectation) {
        return uni -> uni.onItem().transformToUni(item -> {
            if (expectation.test(item)) {
                return Uni.createFrom().item(item);
            } else {
                return Uni.createFrom().failure(expectation.describe(item));
            }
        });
    }

    /**
     * Yields a function to turn an {@link Expectation} into a {@link Uni} and uses an extractor so that expectations
     * work on the correct types (e.g., {@link io.vertx.core.http.HttpResponseHead}) instead of the Mutiny shim types
     * (e.g., {@link io.vertx.mutiny.core.http.HttpResponseHead}).
     *
     * @param extractor the extractor function, often a reference to a {@code getDelegate()} method
     * @param expectation the expectation
     * @return the mapping function
     * @param <T> the element type
     * @param <R> the extracted element type
     */
    static <T, R> Function<Uni<T>, Uni<T>> expectation(Function<T, R> extractor, Expectation<? super R> expectation) {
        return uni -> uni
                .onItem().transformToUni(item -> {
                    R unwrapped = extractor.apply(item);
                    if (expectation.test(unwrapped)) {
                        return Uni.createFrom().item(item);
                    } else {
                        return Uni.createFrom().failure(expectation.describe(unwrapped));
                    }
                });
    }
}
