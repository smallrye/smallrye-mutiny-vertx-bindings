package io.smallrye.reactive.converters.rxjava1;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Before;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToRSPublisherTCK;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

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
        return Optional.of(Single.just(value).delay(DELAY, TimeUnit.MILLISECONDS).observeOn(Schedulers.computation()));
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
                .observeOn(Schedulers.computation());
    }

    @Override
    protected Optional<Single> createInstanceEmittingANullValueImmediately() {
        return Optional.of(Single.just(null));
    }

    @Override
    protected Optional<Single> createInstanceEmittingANullValueAsynchronously() {
        return Optional.of(Single.just(null)
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation()));
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
        return Optional.of(Observable.never().toSingle());
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
