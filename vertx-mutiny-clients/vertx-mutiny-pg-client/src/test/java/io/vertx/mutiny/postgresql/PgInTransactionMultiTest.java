package io.vertx.mutiny.postgresql;

import org.junit.*;
import org.junit.runners.MethodSorters;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.InTransactionMultiTest;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Ignore // TODO FIX ME
public class PgInTransactionMultiTest extends InTransactionMultiTest {

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
