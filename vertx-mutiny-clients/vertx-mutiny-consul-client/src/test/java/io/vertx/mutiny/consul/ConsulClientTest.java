package io.vertx.mutiny.consul;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.KeyValue;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.consul.ConsulClient;

class ConsulClientTest {

    static GenericContainer<?> container = new GenericContainer<>("consul:1.9")
            .withExposedPorts(8500);

    static Vertx vertx;

    @BeforeAll
    static void init() {
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
        ConsulClient client = ConsulClient.create(vertx, new ConsulClientOptions()
                .setHost(container.getHost())
                .setPort(container.getMappedPort(8500)));

        String uuid = UUID.randomUUID().toString();
        String stored = client.putValue("key", uuid)
                .onItem().transformToUni(x -> client.getValue("key"))
                .onItem().transform(KeyValue::getValue)
                .subscribeAsCompletionStage()
                .toCompletableFuture()
                .join();
        assertThat(stored).isEqualTo(uuid);
    }
}
