package io.smallrye.reactive.converters.rxjava2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import io.reactivex.Maybe;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromCompletionStageTCK;

public class MaybeFromCompletionStageTest extends FromCompletionStageTCK<Maybe> {

    private ReactiveTypeConverter<Maybe> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Maybe.class)
                .orElseThrow(() -> new AssertionError("Maybe converter should be found"));
    }

    @Override
    protected ReactiveTypeConverter<Maybe> converter() {
        return converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String getOne(Maybe instance) {
        Maybe<String> maybe = instance.cast(String.class);
        return maybe.blockingGet();
    }

    @Override
    protected Exception getFailure(Maybe instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            instance.blockingGet();
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }

    // Optional tests

    @SuppressWarnings("unchecked")
    @Test
    public void testWithImmediateFullOptional() {
        String uuid = UUID.randomUUID().toString();
        Maybe<String> instance = converter()
                .fromCompletionStage(CompletableFuture.completedFuture(Optional.of(uuid)));
        String res = getOne(instance);
        assertThat(res).isEqualTo(uuid);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWithImmediateEmptyOptional() {
        Maybe<String> instance = converter()
                .fromCompletionStage(CompletableFuture.completedFuture(Optional.empty()));
        String res = getOne(instance);
        assertThat(res).isNull();
        assertThat(instance.isEmpty().blockingGet()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWithAsynchronousFullOptional() {
        String uuid = UUID.randomUUID().toString();
        Maybe<String> instance = converter()
                .fromCompletionStage(CompletableFuture.supplyAsync(() -> Optional.of(uuid)));
        String res = getOne(instance);
        assertThat(res).isEqualTo(uuid);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWithAsynchronousEmptyOptional() {
        Maybe<String> instance = converter()
                .fromCompletionStage(CompletableFuture.supplyAsync(Optional::empty));
        String res = getOne(instance);
        assertThat(res).isNull();
        assertThat(instance.isEmpty().blockingGet()).isTrue();
    }
}
