package io.smallrye.reactive.converters.mutiny;

import java.time.Duration;
import java.util.Optional;

import org.junit.Before;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToCompletionStageTCK;

@SuppressWarnings("rawtypes")
public class UniToCompletionStageTest extends ToCompletionStageTCK<Uni> {

    private ReactiveTypeConverter<Uni> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Uni.class)
                .orElseThrow(() -> new AssertionError("Uni converter should be found"));
    }

    @Override
    protected Optional<Uni> createInstanceEmittingASingleValueImmediately(String value) {
        return Optional.of(Uni.createFrom().item(value));
    }

    @Override
    protected Optional<Uni> createInstanceEmittingASingleValueAsynchronously(String value) {
        return Optional.of(Uni.createFrom().item(value)
                .onItem().delayIt().by(Duration.ofMillis(10)));
    }

    @Override
    protected Uni createInstanceFailingImmediately(RuntimeException e) {
        return Uni.createFrom().failure(e);
    }

    @Override
    protected Uni createInstanceFailingAsynchronously(RuntimeException e) {
        return Uni.createFrom().item("X")
                .onItem().delayIt().by(Duration.ofMillis(10))
                .map(s -> {
                    throw e;
                });
    }

    @Override
    protected Optional<Uni> createInstanceEmittingANullValueImmediately() {
        return Optional.ofNullable(Uni.createFrom().item(() -> null));
    }

    @Override
    protected Optional<Uni> createInstanceEmittingANullValueAsynchronously() {
        return Optional.ofNullable(Uni.createFrom().item(() -> null)
                .onItem().delayIt().by(Duration.ofMillis(10)));
    }

    @Override
    protected Optional<Uni> createInstanceEmittingMultipleValues(String... values) {
        return Optional.empty();
    }

    @Override
    protected Optional<Uni> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2, RuntimeException e) {
        return Optional.empty();
    }

    @Override
    protected Optional<Uni> createInstanceCompletingImmediately() {
        return Optional.of(Uni.createFrom().item((Void) null));
    }

    @Override
    protected Optional<Uni> createInstanceCompletingAsynchronously() {
        return Optional.of(Uni.createFrom().item("X")
                .onItem().delayIt().by(Duration.ofMillis(10))
                .flatMap(s -> Uni.createFrom().item((Void) null)));
    }

    @Override
    protected Optional<Uni> never() {
        return Optional.of(Uni.createFrom().nothing());
    }

    @Override
    protected Optional<Uni> empty() {
        return Optional.of(Uni.createFrom().item((Void) null));
    }

    @Override
    protected ReactiveTypeConverter<Uni> converter() {
        return converter;
    }
}
