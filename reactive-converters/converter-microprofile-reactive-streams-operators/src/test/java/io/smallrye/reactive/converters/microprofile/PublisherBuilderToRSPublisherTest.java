package io.smallrye.reactive.converters.microprofile;

import java.time.Duration;
import java.util.Optional;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.Before;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToRSPublisherTCK;

@SuppressWarnings("rawtypes")
public class PublisherBuilderToRSPublisherTest extends ToRSPublisherTCK<PublisherBuilder> {

    @Before
    public void lookup() {
        converter = Registry.lookup(PublisherBuilder.class)
                .orElseThrow(() -> new AssertionError("PublisherBuilder converter should be found"));
    }

    private ReactiveTypeConverter<PublisherBuilder> converter;

    @Override
    protected Optional<PublisherBuilder> createInstanceEmittingASingleValueImmediately(String value) {
        return Optional.of(ReactiveStreams.of(value));
    }

    @Override
    protected Optional<PublisherBuilder> createInstanceEmittingASingleValueAsynchronously(String value) {
        return Optional.of(ReactiveStreams.fromPublisher(Uni.createFrom().item(value)
                .onItem().delayIt().by(Duration.ofMillis(10))
                .toMulti()));
    }

    @Override
    protected PublisherBuilder createInstanceFailingImmediately(RuntimeException e) {
        return ReactiveStreams.failed(e);
    }

    @Override
    protected PublisherBuilder createInstanceFailingAsynchronously(RuntimeException e) {
        Publisher<String> publisher = Uni.createFrom().item("x")
                .onItem().delayIt().by(Duration.ofMillis(10))
                .toMulti();
        return ReactiveStreams.fromPublisher(publisher)
                .map(s -> {
                    throw e;
                });
    }

    @Override
    protected Optional<PublisherBuilder> createInstanceEmittingANullValueImmediately() {
        return Optional.empty();
    }

    @Override
    protected Optional<PublisherBuilder> createInstanceEmittingANullValueAsynchronously() {
        return Optional.empty();
    }

    @Override
    protected Optional<PublisherBuilder> createInstanceEmittingMultipleValues(String... values) {
        return Optional.of(ReactiveStreams.of(values));
    }

    @Override
    protected Optional<PublisherBuilder> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2,
            RuntimeException e) {
        PublisherBuilder<String> builder = ReactiveStreams.of(v1, v2, "SENTINEL")
                .map(s -> {
                    if ("SENTINEL".equalsIgnoreCase(s)) {
                        throw e;
                    }
                    return s;
                });
        return Optional.of(builder);
    }

    @Override
    protected Optional<PublisherBuilder> createInstanceCompletingImmediately() {
        return Optional.of(ReactiveStreams.empty());
    }

    @Override
    protected Optional<PublisherBuilder> createInstanceCompletingAsynchronously() {
        return Optional.of(ReactiveStreams.empty());
    }

    @Override
    protected Optional<PublisherBuilder> never() {
        return Optional.of(ReactiveStreams.fromPublisher(Multi.createFrom().nothing()));
    }

    @Override
    protected Optional<PublisherBuilder> empty() {
        return Optional.of(ReactiveStreams.empty());
    }

    @Override
    protected ReactiveTypeConverter<PublisherBuilder> converter() {
        return converter;
    }
}
