package io.vertx.mutiny.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.TransactionUniTest;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class PgInTransactionUniTest extends TransactionUniTest {

    public static PostgreSQLContainer<?> container = new PostgreSQLContainer<>();

    @BeforeClass
    public static void init() {
        container.start();
    }

    @AfterClass
    public static void shutdown() {
        container.stop();
    }

    Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();

        PgConnectOptions options = new PgConnectOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        pool = PgPool.pool(vertx, options, new PoolOptions());

        initDb();
    }

    @After
    public void tearDown() {
        pool.close();
        vertx.closeAndAwait();
    }

    @Override
    protected void verifyDuplicateException(Exception e) {
        assertThat(e).hasMessageContaining("duplicate key value violates unique constraint");
    }
}
