package io.vertx.mutiny.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.mutiny.core.Vertx;

public class CircuitBreakerTest {

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
    public void test() {
        CircuitBreaker cb = CircuitBreaker.create("my-circuit-breaker", vertx,
                new CircuitBreakerOptions().setFallbackOnFailure(true));
        String result = cb.executeWithFallbackAndAwait(Uni.createFrom().item("ok"), e -> "nope");
        assertThat(result).isEqualTo("ok");
        result = cb.executeWithFallbackAndAwait(Uni.createFrom().failure(() -> new IllegalArgumentException("boom")),
                e -> "fallback");
        assertThat(result).isEqualTo("fallback");
    }

}
