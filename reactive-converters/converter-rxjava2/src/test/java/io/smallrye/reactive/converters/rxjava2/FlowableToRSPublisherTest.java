package io.smallrye.reactive.converters.rxjava2;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Before;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToRSPublisherTCK;

public class FlowableToRSPublisherTest extends ToRSPublisherTCK<Flowable> {

    private static final int DELAY = 10;
    private ReactiveTypeConverter<Flowable> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Flowable.class)
                .orElseThrow(() -> new AssertionError("Flowable converter should be found"));
    }

    @Override
    protected Optional<Flowable> createInstanceEmittingASingleValueImmediately(String value) {
        return Optional.of(Flowable.just(value));
    }

    @Override
    protected Optional<Flowable> createInstanceEmittingASingleValueAsynchronously(String value) {
        return Optional.of(
                Flowable.just(value).delay(DELAY, TimeUnit.MILLISECONDS).observeOn(Schedulers.io()));
    }

    @Override
    protected Flowable createInstanceFailingImmediately(RuntimeException e) {
        return Flowable.error(e);
    }

    @Override
    protected Flowable createInstanceFailingAsynchronously(RuntimeException e) {
        return Flowable.just("X")
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .map(x -> {
                    throw e;
                })
                .observeOn(Schedulers.io());
    }

    @Override
    protected Optional<Flowable> createInstanceEmittingANullValueImmediately() {
        return Optional.of(Flowable.just("X").map(s -> null));
    }

    @Override
    protected Optional<Flowable> createInstanceEmittingANullValueAsynchronously() {
        return Optional.of(
                Flowable.just("X")
                        .delay(DELAY, TimeUnit.MILLISECONDS)
                        .observeOn(Schedulers.io())
                        .map(s -> null));
    }

    @Override
    protected Optional<Flowable> createInstanceEmittingMultipleValues(String... values) {
        return Optional.of(Flowable.fromArray(values));
    }

    @Override
    protected Optional<Flowable> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2,
            RuntimeException e) {
        Flowable<String> stream = Flowable.create(emitter -> {
            emitter.onNext(v1);
            emitter.onNext(v2);
            emitter.onError(e);
        }, BackpressureStrategy.ERROR);
        return Optional.of(stream);
    }

    @Override
    protected Optional<Flowable> createInstanceCompletingImmediately() {
        return empty();
    }

    @Override
    protected Optional<Flowable> createInstanceCompletingAsynchronously() {
        return Optional.of(Flowable.just("X")
                .delay(10, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .flatMap(x -> Flowable.empty()));
    }

    @Override
    protected Optional<Flowable> never() {
        return Optional.of(Flowable.never());
    }

    @Override
    protected Optional<Flowable> empty() {
        return Optional.of(Flowable.empty());
    }

    @Override
    protected ReactiveTypeConverter<Flowable> converter() {
        return converter;
    }
}
