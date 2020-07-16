package io.smallrye.reactive.converters.mutiny;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToRSPublisherTCK;

@SuppressWarnings("rawtypes")
public class MultiToRSPublisherTest extends ToRSPublisherTCK<Multi> {

    private ReactiveTypeConverter<Multi> converter;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Before
    public void lookup() {
        converter = Registry.lookup(Multi.class)
                .orElseThrow(() -> new AssertionError("Multi converter should be found"));
    }

    @Override
    protected Optional<Multi> createInstanceEmittingASingleValueImmediately(String value) {
        return Optional.of(Multi.createFrom().item(value));
    }

    @Override
    protected Optional<Multi> createInstanceEmittingASingleValueAsynchronously(String value) {
        return Optional.of(Multi.createFrom().item(value)
                .emitOn(executor));
    }

    @Override
    protected Multi createInstanceFailingImmediately(RuntimeException e) {
        return Multi.createFrom().failure(e);
    }

    @Override
    protected Multi createInstanceFailingAsynchronously(RuntimeException e) {
        return Multi.createFrom().item("X")
                .emitOn(executor)
                .map(s -> {
                    throw e;
                });
    }

    @Override
    protected Optional<Multi> createInstanceEmittingANullValueImmediately() {
        return Optional.empty();
    }

    @Override
    protected Optional<Multi> createInstanceEmittingANullValueAsynchronously() {
        return Optional.empty();
    }

    @Override
    protected Optional<Multi> createInstanceEmittingMultipleValues(String... values) {
        return Optional.of(Multi.createFrom().items(values));
    }

    @Override
    protected Optional<Multi> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2, RuntimeException e) {
        Multi<String> stream = Multi.createFrom().emitter(emitter -> {
            emitter.emit(v1);
            emitter.emit(v2);
            emitter.fail(e);
        });
        return Optional.of(stream);
    }

    @Override
    protected Optional<Multi> createInstanceCompletingImmediately() {
        return Optional.of(Multi.createFrom().empty());
    }

    @Override
    protected Optional<Multi> createInstanceCompletingAsynchronously() {
        return Optional.of(Multi.createFrom().item("X")
                .emitOn(executor)
                .flatMap(s -> Multi.createFrom().empty()));
    }

    @Override
    protected Optional<Multi> never() {
        return Optional.of(Multi.createFrom().nothing());
    }

    @Override
    protected Optional<Multi> empty() {
        return Optional.of(Multi.createFrom().empty());
    }

    @Override
    protected ReactiveTypeConverter<Multi> converter() {
        return converter;
    }
}
