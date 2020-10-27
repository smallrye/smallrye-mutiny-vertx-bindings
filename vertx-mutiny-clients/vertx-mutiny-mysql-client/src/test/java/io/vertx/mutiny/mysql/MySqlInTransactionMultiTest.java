package io.vertx.mutiny.mysql;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.testcontainers.containers.GenericContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.InTransactionMultiTest;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;

@Ignore // FIX ME
public class MySqlInTransactionMultiTest extends InTransactionMultiTest {
    private static final String MYSQL_ROOT_PASSWORD = "my-secret-pw";
    private static final String MYSQL_DATABASE = "test";

    @Rule
    public GenericContainer<?> container = new GenericContainer<>("mysql:latest")
            .withExposedPorts(3306)
            .withEnv("MYSQL_ROOT_PASSWORD", MYSQL_ROOT_PASSWORD)
            .withEnv("MYSQL_DATABASE", MYSQL_DATABASE);

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();

        MySQLConnectOptions options = new MySQLConnectOptions()
                .setPort(container.getMappedPort(3306))
                .setHost(container.getContainerIpAddress())
                .setDatabase(MYSQL_DATABASE)
                .setUser("root")
                .setPassword(MYSQL_ROOT_PASSWORD);

        pool = MySQLPool.pool(vertx, options, new PoolOptions());

        initDb();
    }

    @After
    public void tearDown() {
        pool.close();
        vertx.closeAndAwait();
    }
}
