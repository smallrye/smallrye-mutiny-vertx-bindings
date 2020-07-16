package io.smallrye.reactive.converters.rxjava2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToRSPublisherTCK;

public class CompletableToRSPublisherTest extends ToRSPublisherTCK<Completable> {

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
                .observeOn(Schedulers.io());
    }

    @Override
    protected Optional<Completable> createInstanceEmittingANullValueImmediately() {
        return Optional.empty();
    }

    @Override
    protected Optional<Completable> createInstanceEmittingANullValueAsynchronously() {
        return Optional.empty();
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
                .observeOn(Schedulers.io())
                .andThen(Completable.complete()));
    }

    @Override
    protected Optional<Completable> never() {
        return Optional.of(Observable.never().ignoreElements());
    }

    @Override
    protected Optional<Completable> empty() {
        return Optional.of(Completable.complete());
    }

    @Override
    protected ReactiveTypeConverter<Completable> converter() {
        return converter;
    }

    @Test
    public void testToPublisherWithImmediateCompletion() {
        Completable completable = Completable.complete();
        Flowable<String> flowable = Flowable.fromPublisher(converter.toRSPublisher(completable));
        String res = flowable.blockingFirst("DEFAULT");
        assertThat(res).isEqualTo("DEFAULT");
    }

    @Test
    public void testToPublisherWithDelayedCompletion() {
        Completable completable = Single.just("hello").delay(10, TimeUnit.MILLISECONDS).ignoreElement();
        Flowable<String> flowable = Flowable.fromPublisher(converter.toRSPublisher(completable));
        String res = flowable.blockingFirst("DEFAULT");
        assertThat(res).isEqualTo("DEFAULT");
    }
}
