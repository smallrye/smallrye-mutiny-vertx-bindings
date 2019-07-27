package io.vertx.axle.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.pgclient.PgPool;
import io.vertx.axle.sqlclient.Pool;
import io.vertx.axle.sqlclient.RowSet;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class PostGreSQLClientTest {

    @Rule
    public PostgreSQLContainer container = new PostgreSQLContainer();

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void testAxleAPI() {
        PgConnectOptions options = new PgConnectOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        Pool client = PgPool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        RowSet join = client.query("SELECT 1").toCompletableFuture().join();
        assertThat(join).isNotNull();
        assertThat(join.size()).isEqualTo(1);
    }
}
