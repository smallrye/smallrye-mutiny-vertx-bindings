package io.vertx.mutiny.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.mongo.MongoClient;

class MongoClientTest {

    static GenericContainer<?> container = new GenericContainer<>("mongo:4.4")
            .withExposedPorts(27017);

    static Vertx vertx;

    @BeforeAll
    static void init() {
        container.start();
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @AfterEach
    public void tearDown() {
        vertx.closeAndAwait();
        container.stop();
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
