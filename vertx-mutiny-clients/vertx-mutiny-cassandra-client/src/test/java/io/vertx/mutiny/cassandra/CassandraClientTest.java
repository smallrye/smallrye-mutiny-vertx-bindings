package io.vertx.mutiny.cassandra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CassandraContainer;

import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.mutiny.core.Vertx;

class CassandraClientTest {

    static CassandraContainer<?> container = new CassandraContainer<>("cassandra:3.11")
            .withExposedPorts(9042);

    private static Vertx vertx;

    @BeforeAll
    static void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
        container.start();
    }

    @AfterAll
    static void tearDown() {
        vertx.closeAndAwait();
        container.stop();
    }

    @Test
    void testMutinyAPI() {
        CassandraClient client = CassandraClient.create(vertx, new CassandraClientOptions()
                .addContactPoint(container.getHost(), container.getMappedPort(9042)));
    }
}
