package io.vertx.axle.mysql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.MySQLContainer;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.mysqlclient.MySQLPool;
import io.vertx.axle.sqlclient.Pool;
import io.vertx.axle.sqlclient.RowSet;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class MySQLClientTest {
    @Rule
    public MySQLContainer container = new MySQLContainer();

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
        MySQLConnectOptions options = new MySQLConnectOptions()
                .setPort(container.getMappedPort(3306))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        Pool client = MySQLPool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        RowSet join = client.query("SELECT 1").toCompletableFuture().join();
        assertThat(join).isNotNull();
        assertThat(join.size()).isEqualTo(1);
    }
}
