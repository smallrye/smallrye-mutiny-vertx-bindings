package io.vertx.mutiny.cassandra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;

import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.mutiny.core.Vertx;

public class CassandraClientTest {

    @Rule
    public GenericContainer<?> container = new CassandraContainer<>()
            .withExposedPorts(9042);

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
        CassandraClient client = CassandraClient.create(vertx, new CassandraClientOptions()
                .addContactPoint(container.getContainerIpAddress(), container.getMappedPort(9042)));
    }
}
