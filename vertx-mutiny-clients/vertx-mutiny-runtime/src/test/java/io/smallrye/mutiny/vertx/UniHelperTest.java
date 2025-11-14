package io.smallrye.mutiny.vertx;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

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
