package io.vertx.mutiny.cassandra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assume.assumeThat;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.CassandraContainer;

import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.mutiny.core.Vertx;

public class CassandraClientTest {

    @BeforeClass
    public static void beforeAll() {
        assumeThat(System.getProperty("skipInContainerTests"), is(nullValue()));
    }

    @Rule
    public CassandraContainer<?> container = new CassandraContainer<>("cassandra:3.11")
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
