package io.vertx.mutiny.pgclient;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import io.reactiverse.mutiny.pgclient.PgClient;
import io.reactiverse.mutiny.pgclient.PgRowSet;
import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.mutiny.core.Vertx;

public class PostGreSQLClientTest {

    @Rule
    public PostgreSQLContainer<?> container = new PostgreSQLContainer<>();

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testMutinyAPI() {
        PgPoolOptions options = new PgPoolOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword())
                .setMaxSize(5);

        PgClient client = PgClient.pool(vertx, options);

        PgRowSet join = client.preparedQuery("SELECT 1").subscribeAsCompletionStage().join();
        assertThat(join).isNotNull();
        assertThat(join.size()).isEqualTo(1);
    }
}
