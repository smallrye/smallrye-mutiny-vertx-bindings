package io.vertx.mutiny.postgresql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assume.assumeThat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.mutiny.core.Vertx;

public class PGTestBase {

    public static PostgreSQLContainer<?> container = new PostgreSQLContainer<>();

    @BeforeClass
    public static void init() {
        assumeThat(System.getProperty("skipInContainerTests"), is(nullValue()));
        container.start();
    }

    @AfterClass
    public static void shutdown() {
        container.stop();
    }

    Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

}
