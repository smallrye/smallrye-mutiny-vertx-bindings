package io.smallrye.reactive.converters.tck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.UUID;
import java.util.concurrent.*;

import org.junit.Test;

import io.smallrye.reactive.converters.ReactiveTypeConverter;

public abstract class FromCompletionStageTCK<T> {

    protected abstract ReactiveTypeConverter<T> converter();

    protected abstract String getOne(T instance);

    protected abstract Exception getFailure(T instance);

    @Test
    public void testWithImmediateValue() {
        String uuid = UUID.randomUUID().toString();
        T instance = converter()
                .fromCompletionStage(CompletableFuture.completedFuture(uuid));
        String res = getOne(instance);
        if (converter().emitItems()) {
            assertThat(res).isEqualTo(uuid);
        } else {
            assertThat(res).isNull();
        }
    }

    @Test
    public void testWithAsynchronousValue() {
        String uuid = UUID.randomUUID().toString();
        T instance = converter()
                .fromCompletionStage(CompletableFuture.supplyAsync(() -> uuid));
        String res = getOne(instance);
        if (converter().emitItems()) {
            assertThat(res).isEqualTo(uuid);
        } else {
            assertThat(res).isNull();
        }
    }

    @Test
    public void testWithImmediateFailure() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new BoomException());
        T instance = converter()
                .fromCompletionStage(future);

        Exception e = getFailure(instance);
        assertThat(e).isNotNull()
                .isInstanceOf(BoomException.class);
    }

    @Test
    public void testWithAsynchronousFailure() {
        T instance = converter()
                .fromCompletionStage(CompletableFuture.supplyAsync(() -> {
                    throw new BoomException();
                }));

        Exception e = getFailure(instance);
        assertThat(e).isNotNull()
                .isInstanceOf(BoomException.class);
    }

    @Test
    public void testWithImmediateNullValue() {
        CompletionStage<Void> future = CompletableFuture.completedFuture(null);
        if (converter().requireAtLeastOneItem() && !converter().supportNullValue()) {
            //noinspection CatchMayIgnoreException
            try {
                T instance = converter().fromCompletionStage(future);
                getOne(instance);
                fail("Exception expected when completing with `null`");
            } catch (Exception e) {
                assertThat(e).isInstanceOf(NullPointerException.class);
            }
        } else {
            assertThat(getOne(converter().fromCompletionStage(future))).isNull();
        }
    }

    @Test
    public void testWithAsynchronousNullValue() {
        CompletionStage<Void> future = CompletableFuture.supplyAsync(() -> null);
        if (converter().requireAtLeastOneItem() && !converter().supportNullValue()) {
            //noinspection CatchMayIgnoreException
            try {
                getOne(converter().fromCompletionStage(future));
                fail("Exception expected when completing with `null`");
            } catch (Exception e) {
                assertThat(e).isInstanceOf(NullPointerException.class);
            }
        } else {
            assertThat(getOne(converter().fromCompletionStage(future))).isNull();
        }
    }

    @Test
    public void testWhenTheCompletionStageIsCancelled() {
        CompletableFuture<String> future = new CompletableFuture<>();
        T instance = converter().fromCompletionStage(future);
        future.cancel(false);

        Exception exception = getFailure(instance);
        assertThat(exception)
                .isInstanceOf(CancellationException.class);
    }

    @Test
    public void testWithACompletionStageNotCompleting() throws InterruptedException {
        CompletionStage<String> never = new CompletableFuture<>();
        T instance = converter().fromCompletionStage(never);
        CountDownLatch latch = new CountDownLatch(1);
        Future<?> future = Executors.newSingleThreadExecutor().submit(() -> {
            getOne(instance);
            latch.countDown();
        });
        boolean terminated = latch.await(10, TimeUnit.MILLISECONDS);
        future.cancel(true);
        assertThat(terminated).isFalse();
    }

}
