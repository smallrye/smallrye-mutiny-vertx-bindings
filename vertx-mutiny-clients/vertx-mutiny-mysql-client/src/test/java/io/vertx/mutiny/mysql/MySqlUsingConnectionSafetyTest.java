package io.vertx.mutiny.mysql;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assume.assumeThat;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.testcontainers.containers.GenericContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.UsingConnectionSafetyTest;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class MySqlUsingConnectionSafetyTest extends UsingConnectionSafetyTest {

    @BeforeClass
    public static void beforeAll() {
        assumeThat(System.getProperty("skipInContainerTests"), is(nullValue()));
        container = new GenericContainer<>("mysql:8")
                .withExposedPorts(3306)
                .withEnv("MYSQL_ROOT_PASSWORD", MYSQL_ROOT_PASSWORD)
                .withEnv("MYSQL_DATABASE", MYSQL_DATABASE);
    }

    private static final String MYSQL_ROOT_PASSWORD = "my-secret-pw";
    private static final String MYSQL_DATABASE = "test";

    @ClassRule
    public static GenericContainer<?> container;

    private Vertx vertx;
    private int maxSize;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();

        MySQLConnectOptions options = new MySQLConnectOptions()
                .setPort(container.getMappedPort(3306))
                .setHost(container.getContainerIpAddress())
                .setDatabase(MYSQL_DATABASE)
                .setUser("root")
                .setPassword(MYSQL_ROOT_PASSWORD);

        maxSize = 5;
        pool = MySQLPool.pool(vertx, options, new PoolOptions().setMaxSize(maxSize));
    }

    @Override
    protected int getMaxPoolSize() {
        return maxSize;
    }

    @After
    public void tearDown() {
        pool.close();
        vertx.closeAndAwait();
    }
}
