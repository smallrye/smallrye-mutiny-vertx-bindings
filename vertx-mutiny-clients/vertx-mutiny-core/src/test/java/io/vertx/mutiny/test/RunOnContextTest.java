package io.vertx.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;

public class RunOnContextTest {

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testRunOnContext() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        vertx.runOnContext(() -> {
            assertThat(Vertx.currentContext() != null);
            assertThat(Context.isOnVertxThread()).isTrue();
            latch.countDown();
        });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testRunOnContextFromContext() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context context = vertx.getOrCreateContext();
        context.runOnContext(() -> {
            assertThat(Vertx.currentContext() != null);
            assertThat(Context.isOnVertxThread()).isTrue();
            latch.countDown();
        });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }
}
