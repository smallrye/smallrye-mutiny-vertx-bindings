package io.vertx.mutiny.consul;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.KeyValue;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.consul.ConsulClient;

public class ConsulClientTest {

    @Rule
    public GenericContainer container = new GenericContainer("consul")
            .withExposedPorts(8500);

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
        ConsulClient client = ConsulClient.create(vertx, new ConsulClientOptions()
                .setHost(container.getContainerIpAddress())
                .setPort(container.getMappedPort(8500)));

        String uuid = UUID.randomUUID().toString();
        String stored = client.putValue("key", uuid)
                .onItem().produceUni(x -> client.getValue("key"))
                .onItem().apply(KeyValue::getValue)
                .subscribeAsCompletionStage()
                .toCompletableFuture()
                .join();
        assertThat(stored).isEqualTo(uuid);
    }
}
