package io.smallrye.reactive.converters.rxjava2;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Before;

import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToRSPublisherTCK;

public class MaybeToRSPublisherTest extends ToRSPublisherTCK<Maybe> {

    private static final int DELAY = 10;
    private ReactiveTypeConverter<Maybe> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Maybe.class)
                .orElseThrow(() -> new AssertionError("Maybe converter should be found"));
    }

    @Override
    protected Optional<Maybe> createInstanceEmittingASingleValueImmediately(String value) {
        return Optional.of(Maybe.just(value));
    }

    @Override
    protected Optional<Maybe> createInstanceEmittingASingleValueAsynchronously(String value) {
        return Optional.of(Maybe.just(value).delay(DELAY, TimeUnit.MILLISECONDS).observeOn(Schedulers.io()));
    }

    @Override
    protected Maybe createInstanceFailingImmediately(RuntimeException e) {
        return Maybe.error(e);
    }

    @Override
    protected Maybe createInstanceFailingAsynchronously(RuntimeException e) {
        return Maybe.just("X")
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .map(x -> {
                    throw e;
                })
                .observeOn(Schedulers.io());
    }

    @Override
    protected Optional<Maybe> createInstanceEmittingANullValueImmediately() {
        return Optional.of(Maybe.just("x").map(s -> null));
    }

    @Override
    protected Optional<Maybe> createInstanceEmittingANullValueAsynchronously() {
        return Optional.of(Maybe.just("x")
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .map(s -> null)
                .observeOn(Schedulers.io()));
    }

    @Override
    protected Optional<Maybe> createInstanceEmittingMultipleValues(String... values) {
        return Optional.empty();
    }

    @Override
    protected Optional<Maybe> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2,
            RuntimeException e) {
        return Optional.empty();
    }

    @Override
    protected Optional<Maybe> createInstanceCompletingImmediately() {
        return Optional.of(Maybe.empty());
    }

    @Override
    protected Optional<Maybe> createInstanceCompletingAsynchronously() {
        return Optional.of(Maybe.empty().observeOn(Schedulers.io()));
    }

    @Override
    protected Optional<Maybe> never() {
        return Optional.of(Maybe.never());
    }

    @Override
    protected Optional<Maybe> empty() {
        return Optional.of(Maybe.empty());
    }

    @Override
    protected ReactiveTypeConverter<Maybe> converter() {
        return converter;
    }
}
