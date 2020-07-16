package io.smallrye.reactive.converters.reactor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.Before;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToRSPublisherTCK;
import reactor.core.publisher.Mono;

public class MonoToRSPublisherTest extends ToRSPublisherTCK<Mono> {

    private ReactiveTypeConverter<Mono> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Mono.class)
                .orElseThrow(() -> new AssertionError("Mono converter should be found"));
    }

    @Override
    protected Optional<Mono> createInstanceEmittingASingleValueImmediately(String value) {
        return Optional.of(Mono.just(value));
    }

    @Override
    protected Optional<Mono> createInstanceEmittingASingleValueAsynchronously(String value) {
        return Optional.of(Mono.just(value).delayElement(Duration.of(10, ChronoUnit.MILLIS)));
    }

    @Override
    protected Mono createInstanceFailingImmediately(RuntimeException e) {
        return Mono.error(e);
    }

    @Override
    protected Mono createInstanceFailingAsynchronously(RuntimeException e) {
        return Mono.just("X").delayElement(Duration.of(10, ChronoUnit.MILLIS)).map(s -> {
            throw e;
        });
    }

    @Override
    protected Optional<Mono> createInstanceEmittingANullValueImmediately() {
        return Optional.empty();
    }

    @Override
    protected Optional<Mono> createInstanceEmittingANullValueAsynchronously() {
        return Optional.empty();
    }

    @Override
    protected Optional<Mono> createInstanceEmittingMultipleValues(String... values) {
        return Optional.empty();
    }

    @Override
    protected Optional<Mono> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2, RuntimeException e) {
        return Optional.empty();
    }

    @Override
    protected Optional<Mono> createInstanceCompletingImmediately() {
        return Optional.of(Mono.empty());
    }

    @Override
    protected Optional<Mono> createInstanceCompletingAsynchronously() {
        return Optional.of(Mono.just("X").delayElement(Duration.of(10, ChronoUnit.MILLIS))
                .flatMap(s -> Mono.empty()));
    }

    @Override
    protected Optional<Mono> never() {
        return Optional.of(Mono.never());
    }

    @Override
    protected Optional<Mono> empty() {
        return Optional.of(Mono.empty());
    }

    @Override
    protected ReactiveTypeConverter<Mono> converter() {
        return converter;
    }
}
