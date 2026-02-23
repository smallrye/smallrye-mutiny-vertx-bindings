package io.vertx.mutiny.mysql;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.UsingConnectionSafetyTest;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class MySqlUsingConnectionSafetyTest extends UsingConnectionSafetyTest {
    private static final String MYSQL_ROOT_PASSWORD = "my-secret-pw";
    private static final String MYSQL_DATABASE = "test";

    public static GenericContainer<?> container = new GenericContainer<>("mysql:8")
            .withExposedPorts(3306)
            .withEnv("MYSQL_ROOT_PASSWORD", MYSQL_ROOT_PASSWORD)
            .withEnv("MYSQL_DATABASE", MYSQL_DATABASE);

    private Vertx vertx;
    private int maxSize;

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

        MySQLConnectOptions options = new MySQLConnectOptions()
                .setPort(container.getMappedPort(3306))
                .setHost(container.getContainerIpAddress())
                .setDatabase(MYSQL_DATABASE)
                .setUser("root")
                .setPassword(MYSQL_ROOT_PASSWORD);

        maxSize = 5;
        pool = Pool.pool(vertx, options, new PoolOptions().setMaxSize(maxSize));
    }

    @Override
    protected int getMaxPoolSize() {
        return maxSize;
    }

    @AfterEach
    public void tearDown() {
        pool.close();
        vertx.closeAndAwait();
    }
}
