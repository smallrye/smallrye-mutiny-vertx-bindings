package io.vertx.mutiny.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assume.assumeThat;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.mongo.MongoClient;

public class MongoClientTest {

    @BeforeClass
    public static void beforeAll() {
        assumeThat(System.getProperty("skipInContainerTests"), is(nullValue()));
    }

    @Rule
    public GenericContainer<?> container = new GenericContainer<>("mongo:4.4")
            .withExposedPorts(27017);

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
    public void testmutinyAPI() {
        MongoClient client = MongoClient.createShared(vertx, new JsonObject()
                .put("db_name", "mutiny-test")
                .put("connection_string", "mongodb://" + container.getContainerIpAddress()
                        + ":" + container.getMappedPort(27017)));

        JsonObject document = new JsonObject().put("title", "The Hobbit");
        List<JsonObject> list = client.save("books", document)
                .onItem().transformToUni(x -> client.find("books", new JsonObject().put("title", "The Hobbit")))
                .subscribeAsCompletionStage()
                .toCompletableFuture()
                .join();

        assertThat(list).hasSize(1)
                .allMatch(json -> json.getString("title").equalsIgnoreCase("The Hobbit"));
    }
}
