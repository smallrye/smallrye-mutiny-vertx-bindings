package io.vertx.mutiny.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.mutiny.core.Vertx;

public class PGTestBase {

    public static PostgreSQLContainer<?> container = new PostgreSQLContainer<>();

    @BeforeAll
    public static void init() {
        container.start();
    }

    @AfterAll
    public static void shutdown() {
        container.stop();
    }

    Vertx vertx;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @AfterEach
    public void tearDown() {
        vertx.closeAndAwait();
    }

}
