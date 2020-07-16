package io.smallrye.reactive.converters.reactor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.Before;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToRSPublisherTCK;
import reactor.core.publisher.Flux;

public class FluxToRSPublisherTest extends ToRSPublisherTCK<Flux> {

    private ReactiveTypeConverter<Flux> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Flux.class)
                .orElseThrow(() -> new AssertionError("Flux converter should be found"));
    }

    @Override
    protected Optional<Flux> createInstanceEmittingASingleValueImmediately(String value) {
        return Optional.of(Flux.just(value));
    }

    @Override
    protected Optional<Flux> createInstanceEmittingASingleValueAsynchronously(String value) {
        return Optional.of(Flux.just(value).delayElements(Duration.of(10, ChronoUnit.MILLIS)));
    }

    @Override
    protected Flux createInstanceFailingImmediately(RuntimeException e) {
        return Flux.error(e);
    }

    @Override
    protected Flux createInstanceFailingAsynchronously(RuntimeException e) {
        return Flux.just("X").delayElements(Duration.of(10, ChronoUnit.MILLIS)).map(s -> {
            throw e;
        });
    }

    @Override
    protected Optional<Flux> createInstanceEmittingANullValueImmediately() {
        return Optional.empty();
    }

    @Override
    protected Optional<Flux> createInstanceEmittingANullValueAsynchronously() {
        return Optional.empty();
    }

    @Override
    protected Optional<Flux> createInstanceEmittingMultipleValues(String... values) {
        return Optional.of(Flux.fromArray(values));
    }

    @Override
    protected Optional<Flux> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2, RuntimeException e) {
        Flux<String> stream = Flux.create(emitter -> {
            emitter.next(v1);
            emitter.next(v2);
            emitter.error(e);
        });
        return Optional.of(stream);
    }

    @Override
    protected Optional<Flux> createInstanceCompletingImmediately() {
        return Optional.of(Flux.empty());
    }

    @Override
    protected Optional<Flux> createInstanceCompletingAsynchronously() {
        return Optional.of(Flux.just("X").delayElements(Duration.of(10, ChronoUnit.MILLIS))
                .flatMap(s -> Flux.empty()));
    }

    @Override
    protected Optional<Flux> never() {
        return Optional.of(Flux.never());
    }

    @Override
    protected Optional<Flux> empty() {
        return Optional.of(Flux.empty());
    }

    @Override
    protected ReactiveTypeConverter<Flux> converter() {
        return converter;
    }
}
