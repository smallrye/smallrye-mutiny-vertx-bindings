package io.vertx.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

public class ExecuteBlockingTest {

    private Vertx vertx;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testExecuteBlocking() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = vertx.executeBlocking(() -> Uni.createFrom().item(count::incrementAndGet)
                .onItem().delayIt().by(Duration.ofMillis(10))
                .await().indefinitely());
        assertThat(uni.await().indefinitely()).isEqualTo(1);
        assertThat(uni.await().indefinitely()).isEqualTo(2);
        assertThat(uni.await().indefinitely()).isEqualTo(3);
    }

    @Test
    public void testExecuteBlockingNotOrdered() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = vertx.executeBlocking(
                () -> Uni.createFrom().item(count::incrementAndGet).onItem().delayIt().by(Duration.ofMillis(10)),
                false).await().indefinitely();

        assertThat(uni.await().indefinitely()).isEqualTo(1);
        assertThat(uni.await().indefinitely()).isEqualTo(2);
        assertThat(uni.await().indefinitely()).isEqualTo(3);
    }

    @Test
    public void testExecuteBlockingWithFailure() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = vertx.executeBlocking(() -> Uni.createFrom().item(count::incrementAndGet)
                .onItem().failWith(x -> new Exception("boom-" + x)).await().indefinitely());

        assertThatThrownBy(() -> uni.await().indefinitely())
                .hasCauseInstanceOf(Exception.class).hasMessageContaining("boom-1");
        assertThatThrownBy(() -> uni.await().indefinitely())
                .hasCauseInstanceOf(Exception.class).hasMessageContaining("boom-2");
        assertThatThrownBy(() -> uni.await().indefinitely())
                .hasCauseInstanceOf(Exception.class).hasMessageContaining("boom-3");
    }
}
