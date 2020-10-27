package io.vertx.mutiny.postgresql;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.UsingConnectionSafetyTest;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class PgUsingConnectionSafetyTest extends UsingConnectionSafetyTest {

    @Rule
    public PostgreSQLContainer<?> container = new PostgreSQLContainer<>();

    private Vertx vertx;
    private int maxSize;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();

        PgConnectOptions options = new PgConnectOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        maxSize = 5;
        pool = PgPool.pool(vertx, options, new PoolOptions().setMaxSize(maxSize));
    }

    @Override
    protected int getMaxPoolSize() {
        return maxSize;
    }

    @After
    public void tearDown() {
        pool.closeAndAwait();
        vertx.closeAndAwait();
    }
}
