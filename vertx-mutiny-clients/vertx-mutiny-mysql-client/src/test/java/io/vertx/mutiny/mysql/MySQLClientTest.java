package io.vertx.mutiny.mysql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class MySQLClientTest {
    private static final String MYSQL_ROOT_PASSWORD = "my-secret-pw";
    private static final String MYSQL_DATABASE = "test";

    public static GenericContainer<?> container = new GenericContainer<>("mysql:8")
            .withExposedPorts(3306)
            .withEnv("MYSQL_ROOT_PASSWORD", MYSQL_ROOT_PASSWORD)
            .withEnv("MYSQL_DATABASE", MYSQL_DATABASE);

    private Vertx vertx;

    @BeforeAll
    public static void init() {
        container.start();
    }

    @AfterAll
    public static void shutdown() {
        container.stop();
    }

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @AfterEach
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testMutinyAPI() {
        MySQLConnectOptions options = new MySQLConnectOptions()
                .setPort(container.getMappedPort(3306))
                .setHost(container.getContainerIpAddress())
                .setDatabase(MYSQL_DATABASE)
                .setUser("root")
                .setPassword(MYSQL_ROOT_PASSWORD);

        Pool client = Pool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        RowSet<?> join = client.query("SELECT 1")
                .executeAndAwait();
        assertThat(join).isNotNull();
        assertThat(join.size()).isEqualTo(1);
    }
}
