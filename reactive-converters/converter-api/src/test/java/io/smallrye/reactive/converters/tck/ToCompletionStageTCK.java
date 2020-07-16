package io.smallrye.reactive.converters.tck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import org.junit.Test;

import io.smallrye.reactive.converters.ReactiveTypeConverter;

public abstract class ToCompletionStageTCK<T> {

    protected abstract Optional<T> createInstanceEmittingASingleValueImmediately(String value);

    protected abstract Optional<T> createInstanceEmittingASingleValueAsynchronously(String value);

    protected abstract T createInstanceFailingImmediately(RuntimeException e);

    protected abstract T createInstanceFailingAsynchronously(RuntimeException e);

    protected abstract Optional<T> createInstanceEmittingANullValueImmediately();

    protected abstract Optional<T> createInstanceEmittingANullValueAsynchronously();

    protected abstract Optional<T> createInstanceEmittingMultipleValues(String... values);

    protected abstract Optional<T> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2, RuntimeException e);

    protected abstract Optional<T> createInstanceCompletingImmediately();

    protected abstract Optional<T> createInstanceCompletingAsynchronously();

    protected abstract Optional<T> never();

    protected abstract Optional<T> empty();

    protected abstract ReactiveTypeConverter<T> converter();

    @Test
    public void testWithImmediateValue() {
        String uuid = UUID.randomUUID().toString();
        Optional<T> instance = createInstanceEmittingASingleValueImmediately(uuid);
        if (!instance.isPresent()) {
            // Test ignored.
            return;
        }
        CompletionStage<String> stage = converter().toCompletionStage(instance.get());
        String res = stage.toCompletableFuture().join();
        assertThat(res).contains(uuid);
    }

    @Test
    public void testWithAsynchronousValue() {
        String uuid = UUID.randomUUID().toString();
        Optional<T> instance = createInstanceEmittingASingleValueAsynchronously(uuid);
        if (!instance.isPresent()) {
            // Test ignored.
            return;
        }
        CompletionStage<String> stage = converter().toCompletionStage(instance.get());
        String res = stage.toCompletableFuture().join();
        assertThat(res).contains(uuid);
    }

    @Test
    public void testWithImmediateFailure() {
        T instance = createInstanceFailingImmediately(new BoomException());
        CompletionStage<String> stage = converter().toCompletionStage(instance);
        try {
            stage.toCompletableFuture().join();
            fail("Exception expected");
        } catch (CompletionException e) {
            assertThat(e.getCause()).isInstanceOf(BoomException.class);
        }
    }

    @Test
    public void testWithAsynchronousFailure() {
        T instance = createInstanceFailingAsynchronously(new BoomException());
        CompletionStage<String> stage = converter().toCompletionStage(instance);
        try {
            stage.toCompletableFuture().join();
            fail("Exception expected");
        } catch (CompletionException e) {
            assertThat(e.getCause()).isInstanceOf(BoomException.class);
        }
    }

    @Test
    public void testWithImmediateNullValue() {
        Optional<T> optional = createInstanceEmittingANullValueImmediately();
        if (!optional.isPresent()) {
            // Test ignored.
            return;
        }
        CompletionStage<String> stage = converter().toCompletionStage(optional.get());
        assertNullValue(stage);
    }

    @Test
    public void testWithAsynchronousNullValue() {
        Optional<T> optional = createInstanceEmittingANullValueAsynchronously();
        if (!optional.isPresent()) {
            // Test ignored.
            return;
        }
        CompletionStage<String> stage = converter().toCompletionStage(optional.get());
        assertNullValue(stage);
    }

    private void assertNullValue(CompletionStage<String> stage) {
        if (converter().supportNullValue()) {
            String val = stage.toCompletableFuture().join();
            assertThat(val).isNull();
        } else {
            try {
                stage.toCompletableFuture().join();
                fail("Exception expected");
            } catch (CompletionException e) {
                assertThat(e.getCause()).isInstanceOf(NullPointerException.class);
            }
        }
    }

    @Test
    public void testThatOnlyTheFirstValueIsConsidered() {
        String uuid = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        String uuid3 = UUID.randomUUID().toString();
        Optional<T> instance = createInstanceEmittingMultipleValues(uuid, uuid2, uuid3);
        if (!instance.isPresent()) {
            // Test ignored.
            return;
        }
        CompletionStage<String> stage = converter().toCompletionStage(instance.get());
        String res = stage.toCompletableFuture().join();
        assertThat(res).contains(uuid);
    }

    @Test
    public void testThatOnlyTheFirstValueIsConsideredEvenIfAFailureIsEmittedLater() {
        String uuid = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        Optional<T> instance = createInstanceEmittingAMultipleValuesAndFailure(uuid, uuid2, new BoomException());
        if (!instance.isPresent()) {
            // Test ignored.
            return;
        }
        CompletionStage<String> stage = converter().toCompletionStage(instance.get());
        String res = stage.toCompletableFuture().join();
        assertThat(res).contains(uuid);
    }

    @Test
    public void testThatTheCompletionStageIsNotCompletedIfTheInstanceDoesNotEmitSignals() throws InterruptedException {
        Optional<T> instance = never();
        if (!instance.isPresent()) {
            // Test ignored.
            return;
        }
        CompletableFuture<String> stage = converter().<String> toCompletionStage(instance.get()).toCompletableFuture();
        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            stage.join();
            latch.countDown();
        });

        boolean terminated = latch.await(10, TimeUnit.MILLISECONDS);
        stage.cancel(true);

        assertThat(terminated).isFalse();
    }

    @Test
    public void testWithEmptyStream() {
        Optional<T> instance = empty();
        if (!instance.isPresent()) {
            // Test ignored
            return;
        }
        CompletionStage<String> stage = converter().toCompletionStage(instance.get());
        String val = stage.toCompletableFuture().join();
        assertThat(val).isNull();
    }

    @Test
    public void testStreamCompletingImmediately() {
        Optional<T> instance = createInstanceCompletingImmediately();
        if (!instance.isPresent()) {
            // Test ignored
            return;
        }
        CompletionStage<String> stage = converter().toCompletionStage(instance.get());
        String val = stage.toCompletableFuture().join();
        assertThat(val).isNull();
    }

    @Test
    public void testStreamCompletingAsynchronously() {
        Optional<T> instance = createInstanceCompletingAsynchronously();
        if (!instance.isPresent()) {
            // Test ignored
            return;
        }
        CompletionStage<String> stage = converter().toCompletionStage(instance.get());
        String val = stage.toCompletableFuture().join();
        assertThat(val).isNull();
    }

}
