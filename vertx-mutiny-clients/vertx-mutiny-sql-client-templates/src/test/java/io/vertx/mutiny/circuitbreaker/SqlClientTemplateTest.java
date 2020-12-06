package io.vertx.mutiny.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.templates.SqlTemplate;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.PoolOptions;

public class SqlClientTemplateTest {

    @Rule
    public PostgreSQLContainer<?> container = new PostgreSQLContainer<>();

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void test() {
        PgConnectOptions options = new PgConnectOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        Pool client = PgPool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        Map<String, Object> parameters = Collections.singletonMap("id", 1);

        assertThatThrownBy(() -> SqlTemplate
                .forQuery(client, "SELECT * FROM users WHERE id=#{id}")
                .executeAndAwait(parameters)).isInstanceOf(PgException.class); // Table not created.
    }

}
