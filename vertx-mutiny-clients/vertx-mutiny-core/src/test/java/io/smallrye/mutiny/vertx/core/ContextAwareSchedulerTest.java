package io.smallrye.mutiny.vertx.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.ContextInternal;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;

public class ContextAwareSchedulerTest {

    Vertx vertx;
    ScheduledExecutorService delegate;

    @Before
    public void setup() {
        vertx = Vertx.vertx();
        delegate = Executors.newSingleThreadScheduledExecutor();
    }

    @After
    public void teardown() {
        delegate.shutdownNow();
        vertx.closeAndAwait();
    }

    private boolean isDuplicate(Context ctx) {
        return (ctx != null) && ((ContextInternal) ctx.getDelegate()).isDuplicate();
    }

    @Test
    public void rejectNullParameters() {
        assertThatThrownBy(() -> ContextAwareScheduler.delegatingTo(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("The delegate executor cannot be null");

        assertThatThrownBy(() -> ContextAwareScheduler.delegatingTo(delegate)
                .withContext(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("The context cannot be null");

        assertThatThrownBy(() -> ContextAwareScheduler.delegatingTo(delegate)
                .withGetOrCreateContext(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("The Vertx object cannot be null");

        assertThatThrownBy(() -> ContextAwareScheduler.delegatingTo(delegate)
                .withGetOrCreateContextOnCurrentThread(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("The Vertx object cannot be null");
    }

    @Test
    public void executor_getOrCreateContext_no_context() throws InterruptedException {
        ScheduledExecutorService scheduler = ContextAwareScheduler
                .delegatingTo(delegate)
                .withGetOrCreateContext(vertx);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean();
        scheduler.execute(() -> {
            ok.set(isDuplicate(Vertx.currentContext()));
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(ok).isTrue();
    }

    @Test
    public void executor_getOrCreateContext_existing_context() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean();

        vertx.runOnContext(() -> {
            ScheduledExecutorService scheduler = ContextAwareScheduler
                    .delegatingTo(delegate)
                    .withGetOrCreateContext(vertx);

            scheduler.execute(() -> {
                Context ctx = Vertx.currentContext();
                ok.set(isDuplicate(ctx));
                latch.countDown();
            });
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(ok).isTrue();
    }

    @Test
    public void executor_immediate_getOrCreateContext_no_context() throws InterruptedException {
        ScheduledExecutorService scheduler = ContextAwareScheduler
                .delegatingTo(delegate)
                .withGetOrCreateContextOnCurrentThread(vertx);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean();
        scheduler.execute(() -> {
            ok.set(isDuplicate(Vertx.currentContext()));
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(ok).isTrue();
    }

    @Test
    public void executor_immediate_getOrCreateContext_existing_context() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean();

        vertx.runOnContext(() -> {
            ScheduledExecutorService scheduler = ContextAwareScheduler
                    .delegatingTo(delegate)
                    .withGetOrCreateContextOnCurrentThread(vertx);
            Vertx.currentContext().put("foo", "bar");

            scheduler.execute(() -> {
                Context ctx = Vertx.currentContext();
                ok.set(isDuplicate(ctx) && "bar".equals(ctx.get("foo")));
                latch.countDown();
            });
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(ok).isTrue();
    }

    @Test
    public void executor_requiredCurrentContext_ok() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean();

        vertx.runOnContext(() -> {
            ScheduledExecutorService scheduler = ContextAwareScheduler
                    .delegatingTo(delegate)
                    .withCurrentContext();

            scheduler.execute(() -> {
                Context ctx = Vertx.currentContext();
                ok.set(isDuplicate(ctx));
                latch.countDown();
            });
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(ok).isTrue();
    }

    @Test
    public void executor_requiredCurrentContext_fail() throws InterruptedException {
        assertThatThrownBy(() -> ContextAwareScheduler
                .delegatingTo(delegate)
                .withCurrentContext())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("There is no Vert.x context in the current thread:");
    }

    @Test
    public void rejectManyExecutorMethods() {
        ScheduledExecutorService scheduler = ContextAwareScheduler.delegatingTo(delegate)
                .withGetOrCreateContext(vertx);

        assertThatThrownBy(() -> scheduler.submit(() -> 123))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> scheduler.invokeAll(List.of(() -> 123)))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> scheduler.invokeAny(List.of(() -> 123)))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(scheduler::shutdownNow)
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(scheduler::shutdown)
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> scheduler.schedule(() -> 123, 100, TimeUnit.MILLISECONDS))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> scheduler.awaitTermination(100, TimeUnit.MILLISECONDS))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(scheduler.isShutdown()).isFalse();
        assertThat(scheduler.isTerminated()).isFalse();
    }

    @Test
    public void schedule() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean();

        Context context = vertx.getOrCreateContext();
        context.put("foo", "bar");
        ScheduledExecutorService scheduler = ContextAwareScheduler.delegatingTo(delegate)
                .withContext(context);

        scheduler.schedule(() -> {
            Context ctx = Vertx.currentContext();
            ok.set(isDuplicate(ctx) && "bar".equals(ctx.get("foo")));
            latch.countDown();
        }, 100, TimeUnit.MILLISECONDS);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(ok).isTrue();
    }

    @Test
    public void scheduleAtFixedRate() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean();

        Context context = vertx.getOrCreateContext();
        context.put("foo", "bar");
        ScheduledExecutorService scheduler = ContextAwareScheduler.delegatingTo(delegate)
                .withContext(context);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            Context ctx = Vertx.currentContext();
            ok.set(isDuplicate(ctx) && "bar".equals(ctx.get("foo")));
            latch.countDown();
        }, 10, 1000, TimeUnit.MILLISECONDS);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        future.cancel(true);
        assertThat(ok).isTrue();
    }

    @Test
    public void scheduleWithFixedDelay() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ok = new AtomicBoolean();

        Context context = vertx.getOrCreateContext();
        context.put("foo", "bar");
        ScheduledExecutorService scheduler = ContextAwareScheduler.delegatingTo(delegate)
                .withContext(context);

        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> {
            Context ctx = Vertx.currentContext();
            ok.set(isDuplicate(ctx) && "bar".equals(ctx.get("foo")));
            latch.countDown();
        }, 10, 1000, TimeUnit.MILLISECONDS);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        future.cancel(true);
        assertThat(ok).isTrue();
    }

    @Test
    public void usage_delay() throws InterruptedException {
        ScheduledExecutorService scheduler = ContextAwareScheduler.delegatingTo(delegate)
                .withGetOrCreateContext(vertx);

        Integer res = Uni.createFrom().item(123)
                .onItem().delayIt().onExecutor(scheduler).by(Duration.ofMillis(10))
                .onItem().transformToUni(n -> {
                    Context ctx = Vertx.currentContext();
                    if (isDuplicate(ctx)) {
                        ctx.put("foo", 58);
                        return Uni.createFrom().item(n);
                    } else {
                        return Uni.createFrom().failure(new IllegalStateException("No context"));
                    }
                })
                .onItem().ignore().andContinueWith(() -> Vertx.currentContext().get("foo"))
                .await().atMost(Duration.ofSeconds(5));

        assertThat(res)
                .isNotNull()
                .isEqualTo(58);
    }

    @Test
    public void usage_verticle() {
        class MyVerticle extends AbstractVerticle {

            @Override
            public Uni<Void> asyncStart() {
                vertx.getOrCreateContext().put("foo", "bar");

                ScheduledExecutorService scheduler = ContextAwareScheduler.delegatingTo(delegate)
                        .withCurrentContext();

                return Uni.createFrom().voidItem()
                        .onItem().delayIt().onExecutor(scheduler).by(Duration.ofMillis(10))
                        .onItem().transformToUni(v -> {
                            Context ctx = vertx.getOrCreateContext();
                            if (isDuplicate(ctx)) {
                                if ("bar".equals(ctx.get("foo"))) {
                                    return Uni.createFrom().voidItem();
                                } else {
                                    return Uni.createFrom().failure(new IllegalStateException("No data in context"));
                                }
                            } else {
                                return Uni.createFrom().failure(new IllegalStateException("No context"));
                            }
                        });
            }
        }

        vertx.deployVerticle(new MyVerticle()).await().atMost(Duration.ofSeconds(5));
    }
}
