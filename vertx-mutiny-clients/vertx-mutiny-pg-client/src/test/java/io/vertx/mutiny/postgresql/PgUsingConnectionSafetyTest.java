package io.vertx.mutiny.postgresql;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.UsingConnectionSafetyTest;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class PgUsingConnectionSafetyTest extends UsingConnectionSafetyTest {

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
    private int maxSize;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();

        PgConnectOptions options = new PgConnectOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        maxSize = 5;
        pool = Pool.pool(vertx, options, new PoolOptions().setMaxSize(maxSize));
    }

    @Override
    protected int getMaxPoolSize() {
        return maxSize;
    }

    @AfterEach
    public void tearDown() {
        pool.closeAndAwait();
        vertx.closeAndAwait();
    }
}
