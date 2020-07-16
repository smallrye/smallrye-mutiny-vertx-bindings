package io.smallrye.reactive.converters.rxjava3;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import io.reactivex.rxjava3.core.Completable;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromRSPublisherTCK;

@SuppressWarnings("ConstantConditions")
public class CompletableFromRSPublisherTest extends FromRSPublisherTCK<Completable> {

    private ReactiveTypeConverter<Completable> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Completable.class)
                .orElseThrow(() -> new AssertionError("Completable converter should be found"));
    }

    @Override
    protected ReactiveTypeConverter<Completable> converter() {
        return converter;
    }

    @Override
    protected String getOne(Completable instance) {
        instance.blockingAwait();
        return null;
    }

    @Override
    protected Exception getFailure(Completable instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            instance.blockingAwait();
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }

    @Override
    protected List<String> getAll(Completable instance) {
        instance.blockingAwait();
        return Collections.emptyList();
    }

    @Override
    protected void consume(Completable instance) {
        instance.blockingAwait();
    }

    @Test
    public void testWithImmediateCompletion() {
        AtomicBoolean reference = new AtomicBoolean();
        Completable completable = converter
                .fromCompletionStage(CompletableFuture.runAsync(() -> {
                }));
        completable
                .doOnComplete(() -> reference.set(true))
                .blockingAwait();
        assertThat(reference).isTrue();
    }

    @Test
    public void testWithAsynchronousCompletion() {
        AtomicBoolean reference = new AtomicBoolean();
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        });
        Completable completable = converter
                .fromCompletionStage(future);
        completable
                .doOnComplete(() -> reference.set(true))
                .blockingAwait();
        assertThat(reference).isTrue();
    }

}
