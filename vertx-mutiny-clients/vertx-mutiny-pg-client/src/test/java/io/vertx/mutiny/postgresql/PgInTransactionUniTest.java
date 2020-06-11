package io.vertx.mutiny.postgresql;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.InTransactionUniTest;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class PgInTransactionUniTest extends InTransactionUniTest {

    @Rule
    public PostgreSQLContainer<?> container = new PostgreSQLContainer<>();

    private Vertx vertx;

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
}
