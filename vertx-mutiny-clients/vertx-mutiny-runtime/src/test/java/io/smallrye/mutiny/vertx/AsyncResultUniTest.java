package io.smallrye.mutiny.vertx;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

class AsyncResultUniTest {

    @Test
    void errorThrownByTheSupplierIsPropagatedAsFailure() {
        Uni<String> uni = AsyncResultUni.fromFuture(() -> {
            throw new AssertionError("boom");
        });
        AtomicReference<Throwable> failure = new AtomicReference<>();

        assertDoesNotThrow(() -> uni.subscribe().with(item -> fail("No item expected"), failure::set));
        assertInstanceOf(AssertionError.class, failure.get());
        assertEquals("boom", failure.get().getMessage());
    }

    @Test
    void errorThrownDownstreamIsNotSwallowed() {
        AtomicReference<Throwable> dropped = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(dropped::set);
        try {
            // The handler is called synchronously, so the downstream subscriber is called within subscribe()
            AsyncResultUni.<String> toUni(handler -> handler.handle(Future.succeededFuture("ok"))).subscribe()
                    .withSubscriber(new UniSubscriber<String>() {
                        @Override
                        public void onSubscribe(UniSubscription subscription) {
                        }

                        @Override
                        public void onItem(String item) {
                            throw new StackOverflowError("boom");
                        }

                        @Override
                        public void onFailure(Throwable failure) {
                        }
                    });

            assertInstanceOf(StackOverflowError.class, dropped.get());
        } finally {
            Infrastructure.resetDroppedExceptionHandler();
        }
    }

    @Test
    void completionExceptionIsUnwrapped() {
        Uni<String> uni = AsyncResultUni.fromFuture(
                () -> Future.failedFuture(new CompletionException(new IOException("boom"))));
        AtomicReference<Throwable> failure = new AtomicReference<>();

        uni.subscribe().with(item -> fail("No item expected"), failure::set);

        assertInstanceOf(IOException.class, failure.get());
        assertEquals("boom", failure.get().getMessage());
    }

    @Test
    void outcomeIsDispatchedOnTheContextOfTheFuture() {
        Vertx vertx = Vertx.vertx();
        try {
            Context context = vertx.getOrCreateContext();
            Future<String> future = context.executeBlocking(() -> "ok");
            while (!future.isComplete()) {
                LockSupport.parkNanos(Duration.ofMillis(1).toNanos());
            }
            AtomicReference<String> deliveryThread = new AtomicReference<>();

            // Subscribed from the test thread (no Vert.x context) to an already completed future.
            // The delay in onSubscribe gives any intermediate asynchronous hop time to complete before the result is consumed.
            String item = AsyncResultUni.fromFuture(() -> future)
                    .onSubscription().invoke(() -> LockSupport.parkNanos(Duration.ofMillis(100).toNanos()))
                    .onItem().invoke(() -> deliveryThread.set(Thread.currentThread().getName()))
                    .await().atMost(Duration.ofSeconds(5));

            assertEquals("ok", item);
            assertTrue(deliveryThread.get().startsWith("vert.x-eventloop-thread-"),
                    "Delivered on " + deliveryThread.get());
        } finally {
            vertx.close().await();
        }
    }
}
