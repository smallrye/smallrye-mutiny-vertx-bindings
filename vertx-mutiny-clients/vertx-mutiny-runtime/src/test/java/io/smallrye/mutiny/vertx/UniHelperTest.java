package io.smallrye.mutiny.vertx;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

class UniHelperTest {

    @Test
    void test_toFuture() {
        Uni<String> uni = Uni.createFrom().item("Ok");
        Future<String> future = UniHelper.toFuture(uni);

        assertTrue(future.isComplete());
        assertTrue(future.succeeded());
        assertEquals("Ok", future.await());
    }

    @Test
    void test_toPromise() {
        Uni<String> uni = Uni.createFrom().item("Ok");
        Promise<String> promise = UniHelper.toPromise(uni);
        Future<String> future = promise.future();

        assertTrue(future.isComplete());
        assertTrue(future.succeeded());
        assertEquals("Ok", future.await());
    }

    @Test
    void test_toHandlerOfPromise() {
        Uni<String> uni = Uni.createFrom().item("Ok");
        Handler<Promise<String>> handler = UniHelper.toHandlerOfPromise(uni);
        Promise<String> promise = Promise.promise();
        Future<String> future = promise.future();
        handler.handle(promise);

        assertTrue(future.isComplete());
        assertTrue(future.succeeded());
        assertEquals("Ok", future.await());
    }

    @Test
    void test_toUniOfFuture() {
        Uni<String> uni = UniHelper.toUni(Future.succeededFuture("Ok"));
        assertEquals("Ok", uni.await().atMost(Duration.ofSeconds(1)));
    }

    @Test
    void test_toUniOfFutureDispatchesOnTheContextOfTheFuture() throws TimeoutException {
        Vertx vertx = Vertx.vertx();
        try {
            Context context = vertx.getOrCreateContext();
            Future<String> future = context.executeBlocking(() -> "Ok");
            future.await(Duration.ofSeconds(5));
            AtomicReference<Context> deliveryContext = new AtomicReference<>();
            AtomicReference<String> deliveryThread = new AtomicReference<>();

            // Subscribed from the test thread (no Vert.x context) to an already completed future.
            // The delay in onSubscribe gives any intermediate asynchronous hop time to complete before the result is consumed.
            String item = UniHelper.toUni(future)
                    .onSubscription().invoke(() -> LockSupport.parkNanos(Duration.ofMillis(100).toNanos()))
                    .onItem().invoke(() -> {
                        deliveryContext.set(Vertx.currentContext());
                        deliveryThread.set(Thread.currentThread().getName());
                    })
                    .await().atMost(Duration.ofSeconds(5));

            assertEquals("Ok", item);
            assertSame(context, deliveryContext.get(), "Delivered on " + deliveryThread.get());
        } finally {
            vertx.close().await();
        }
    }

    @Test
    void test_toUniOfNullFutureFailsEagerly() {
        assertThrows(IllegalArgumentException.class, () -> UniHelper.toUni((Future<String>) null));
    }

    @Test
    void test_toUniOfFutureUnwrapsCompletionException() {
        Uni<String> uni = UniHelper.toUni(Future.failedFuture(new CompletionException(new IOException("boom"))));
        AtomicReference<Throwable> failure = new AtomicReference<>();

        uni.subscribe().with(item -> fail("No item expected"), failure::set);

        assertInstanceOf(IOException.class, failure.get());
        assertEquals("boom", failure.get().getMessage());
    }

    @Test
    void test_toSubscriber() {
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Supplier<UniSubscriber<String>> supplier = () -> UniHelper.<String> toSubscriber(ar -> {
            if (ar.succeeded()) {
                result.set(ar.result());
            } else {
                failure.set(ar.cause());
            }
        });

        supplier.get().onItem("Ok");
        assertEquals("Ok", result.get());
        assertNull(failure.get());

        result.set(null);
        failure.set(null);

        supplier.get().onFailure(new RuntimeException("Yolo"));
        assertNull(result.get());
        assertInstanceOf(RuntimeException.class, failure.get());
        assertEquals("Yolo", failure.get().getMessage());
    }
}
