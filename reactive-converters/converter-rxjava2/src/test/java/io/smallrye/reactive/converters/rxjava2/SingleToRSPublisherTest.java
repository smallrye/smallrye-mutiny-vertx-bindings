package io.smallrye.reactive.converters.rxjava2;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Before;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToRSPublisherTCK;

public class SingleToRSPublisherTest extends ToRSPublisherTCK<Single> {

    private static final int DELAY = 10;
    private ReactiveTypeConverter<Single> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Single.class)
                .orElseThrow(() -> new AssertionError("Single converter should be found"));
    }

    @Override
    protected Optional<Single> createInstanceEmittingASingleValueImmediately(String value) {
        return Optional.of(Single.just(value));
    }

    @Override
    protected Optional<Single> createInstanceEmittingASingleValueAsynchronously(String value) {
        return Optional.of(Single.just(value).delay(DELAY, TimeUnit.MILLISECONDS).observeOn(Schedulers.io()));
    }

    @Override
    protected Single createInstanceFailingImmediately(RuntimeException e) {
        return Single.error(e);
    }

    @Override
    protected Single createInstanceFailingAsynchronously(RuntimeException e) {
        return Single.just("X")
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .map(x -> {
                    throw e;
                })
                .observeOn(Schedulers.io());
    }

    @Override
    protected Optional<Single> createInstanceEmittingANullValueImmediately() {
        return Optional.of(Single.just("x").map(s -> null));
    }

    @Override
    protected Optional<Single> createInstanceEmittingANullValueAsynchronously() {
        return Optional.of(Single.just("x")
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .map(s -> null)
                .observeOn(Schedulers.io()));
    }

    @Override
    protected Optional<Single> createInstanceEmittingMultipleValues(String... values) {
        return Optional.empty();
    }

    @Override
    protected Optional<Single> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2,
            RuntimeException e) {
        return Optional.empty();
    }

    @Override
    protected Optional<Single> createInstanceCompletingImmediately() {
        return Optional.empty();
    }

    @Override
    protected Optional<Single> createInstanceCompletingAsynchronously() {
        return Optional.empty();
    }

    @Override
    protected Optional<Single> never() {
        return Optional.of(Observable.never().singleOrError());
    }

    @Override
    protected Optional<Single> empty() {
        return Optional.empty();
    }

    @Override
    protected ReactiveTypeConverter<Single> converter() {
        return converter;
    }
}
