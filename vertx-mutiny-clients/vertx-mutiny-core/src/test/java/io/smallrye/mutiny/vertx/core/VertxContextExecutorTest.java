package io.smallrye.mutiny.vertx.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;

public class VertxContextExecutorTest {

    Vertx vertx;

    @Before
    public void prepare() {
        vertx = Vertx.vertx();
    }

    @After
    public void cleanup() {
        vertx.closeAndAwait();
    }

    @Test
    public void with_explicit_context() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Context> box = new AtomicReference<>();

        Context context = vertx.getOrCreateContext();
        VertxContextExecutor.of(context).execute(() -> {
            box.set(Vertx.currentContext());
            latch.countDown();
        });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(box.get())
                .isNotNull()
                .isEqualTo(context);
    }

    @Test
    public void with_vertx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Context> box = new AtomicReference<>();

        VertxContextExecutor.of(vertx).execute(() -> {
            box.set(Vertx.currentContext());
            latch.countDown();
        });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(box.get()).isNotNull();
    }

    @Test
    public void from_non_vertx_caller() {
        assertThatThrownBy(VertxContextExecutor::fromCaller)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Not called from a Vert.x context");
    }

    @Test
    public void from_vertx_caller() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Context> box = new AtomicReference<>();

        Context context = vertx.getOrCreateContext();
        context.runOnContext(() -> VertxContextExecutor.fromCaller().execute(() -> {
            box.set(Vertx.currentContext());
            latch.countDown();
        }));

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(box.get())
                .isNotNull()
                .isEqualTo(context);
    }

    @Test
    public void verticle_emitOn_from_caller() {
        V1 verticle = new V1();
        vertx.deployVerticleAndAwait(verticle);
        assertThat(verticle.emitterId).contains("vert.x-eventloop-thread");
        assertThat(verticle.subscriberId).doesNotContain("vert.x-eventloop-thread");
    }

    @Test
    public void verticle_emitOn_from_vertx() {
        V2 verticle = new V2();
        vertx.deployVerticleAndAwait(verticle);
        assertThat(verticle.emitterId)
                .contains("vert.x-eventloop-thread")
                .isEqualTo(verticle.subscriberId);
    }

    @Test
    public void verticle_emitOn_from_vertx_context_passing() {
        V3 verticle = new V3();
        vertx.deployVerticleAndAwait(verticle);
        assertThat(verticle.message)
                .isNotNull()
                .isEqualTo("Yolo!");
    }

    @Test
    public void verticle_emitOn_from_vertx_context_passing_not_working_with_worker_subscription() {
        V4 verticle = new V4();
        vertx.deployVerticleAndAwait(verticle);
        assertThat(verticle.message).isNull();
    }

    @Test
    public void verticle_emitOn_from_explicit_context_passing_working_with_worker_subscription() {
        V5 verticle = new V5();
        vertx.deployVerticleAndAwait(verticle);
        assertThat(verticle.message)
                .isNotNull()
                .isEqualTo("Yolo!");
    }

    static class V1 extends AbstractVerticle {

        volatile String emitterId;
        volatile String subscriberId;

        @Override
        public Uni<Void> asyncStart() {
            return Uni.createFrom().item("yolo")
                    .onItem().invoke(() -> subscriberId = Thread.currentThread().getName())
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                    .emitOn(VertxContextExecutor.fromCaller())
                    .onItem().invoke(() -> emitterId = Thread.currentThread().getName())
                    .replaceWithVoid();
        }
    }

    static class V2 extends AbstractVerticle {

        volatile String emitterId;
        volatile String subscriberId;

        @Override
        public Uni<Void> asyncStart() {
            this.subscriberId = Thread.currentThread().getName();
            return Uni.createFrom().item("yolo")
                    .onItem().invoke(() -> subscriberId = Thread.currentThread().getName())
                    .emitOn(VertxContextExecutor.of(vertx))
                    .onItem().invoke(() -> emitterId = Thread.currentThread().getName())
                    .replaceWithVoid();
        }
    }

    static class V3 extends AbstractVerticle {

        volatile String message;

        @Override
        public Uni<Void> asyncStart() {
            Vertx.currentContext().putLocal("msg", "Yolo!");
            return Uni.createFrom().item("yolo")
                    .emitOn(VertxContextExecutor.of(vertx))
                    .onItem().invoke(() -> this.message = Vertx.currentContext().getLocal("msg"))
                    .replaceWithVoid();
        }
    }

    static class V4 extends AbstractVerticle {

        volatile String message;

        @Override
        public Uni<Void> asyncStart() {
            Vertx.currentContext().putLocal("msg", "Yolo!");
            return Uni.createFrom().item("yolo")
                    .emitOn(VertxContextExecutor.of(vertx))
                    .onItem().invoke(() -> this.message = Vertx.currentContext().getLocal("msg"))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                    .replaceWithVoid();
        }
    }

    static class V5 extends AbstractVerticle {

        volatile String message;

        @Override
        public Uni<Void> asyncStart() {
            Context context = Vertx.currentContext();
            context.putLocal("msg", "Yolo!");
            return Uni.createFrom().item("yolo")
                    .emitOn(VertxContextExecutor.of(context))
                    .onItem().invoke(() -> this.message = Vertx.currentContext().getLocal("msg"))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                    .replaceWithVoid();
        }
    }
}
