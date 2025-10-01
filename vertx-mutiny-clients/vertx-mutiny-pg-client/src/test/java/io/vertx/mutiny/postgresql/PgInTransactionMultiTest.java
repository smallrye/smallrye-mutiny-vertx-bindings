package io.vertx.mutiny.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.TransactionMultiTest;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class PgInTransactionMultiTest extends TransactionMultiTest {

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

        PgConnectOptions options = new PgConnectOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        pool = Pool.pool(vertx, options, new PoolOptions());

        initDb();
    }

    @AfterEach
    public void tearDown() {
        pool.close();
        vertx.closeAndAwait();
    }

    @Override
    protected void verifyDuplicateException(Exception e) {
        assertThat(e).hasMessageContaining("duplicate key value violates unique constraint");
    }
}
