package io.smallrye.reactive.converters.rxjava1;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Before;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToCompletionStageTCK;
import rx.Completable;
import rx.Observable;
import rx.schedulers.Schedulers;

public class CompletableToCompletionStageTest extends ToCompletionStageTCK<Completable> {

    private static final int DELAY = 10;
    private ReactiveTypeConverter<Completable> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Completable.class)
                .orElseThrow(() -> new AssertionError("Completable converter should be found"));
    }

    @Override
    protected Optional<Completable> createInstanceEmittingASingleValueImmediately(String value) {
        return Optional.empty();
    }

    @Override
    protected Optional<Completable> createInstanceEmittingASingleValueAsynchronously(String value) {
        return Optional.empty();
    }

    @Override
    protected Completable createInstanceFailingImmediately(RuntimeException e) {
        return Completable.error(e);
    }

    @Override
    protected Completable createInstanceFailingAsynchronously(RuntimeException e) {
        return Completable.complete()
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .andThen(Completable.error(e))
                .observeOn(Schedulers.computation());
    }

    @Override
    protected Optional<Completable> createInstanceEmittingANullValueImmediately() {
        return Optional.of(Completable.complete());
    }

    @Override
    protected Optional<Completable> createInstanceEmittingANullValueAsynchronously() {
        return Optional.of(Completable.complete()
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation()));
    }

    @Override
    protected Optional<Completable> createInstanceEmittingMultipleValues(String... values) {
        return Optional.empty();
    }

    @Override
    protected Optional<Completable> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2,
            RuntimeException e) {
        return Optional.empty();
    }

    @Override
    protected Optional<Completable> createInstanceCompletingImmediately() {
        return empty();
    }

    @Override
    protected Optional<Completable> createInstanceCompletingAsynchronously() {
        return Optional.of(Completable
                .complete()
                .delay(10, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .andThen(Completable.complete()));
    }

    @Override
    protected Optional<Completable> never() {
        return Optional.of(Observable.never().toCompletable());
    }

    @Override
    protected Optional<Completable> empty() {
        return Optional.of(Completable.complete());
    }

    @Override
    protected ReactiveTypeConverter<Completable> converter() {
        return converter;
    }
}
