package io.vertx.mutiny.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.templates.SqlTemplate;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.PoolOptions;

public class SqlClientTemplateTest {

    public PostgreSQLContainer<?> container = new PostgreSQLContainer<>();

    private Vertx vertx;

    @BeforeEach
    public void setUp() {
        container.start();
        vertx = Vertx.vertx();
    }

    @AfterEach
    public void tearDown() {
        vertx.closeAndAwait();
        container.stop();
    }

    @Test
    public void test() {
        PgConnectOptions options = new PgConnectOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        Pool client = Pool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        Map<String, Object> parameters = Collections.singletonMap("id", 1);

        assertThatThrownBy(() -> SqlTemplate
                .forQuery(client, "SELECT * FROM users WHERE id=#{id}")
                .executeAndAwait(parameters)).isInstanceOf(PgException.class); // Table not created.
    }

}
