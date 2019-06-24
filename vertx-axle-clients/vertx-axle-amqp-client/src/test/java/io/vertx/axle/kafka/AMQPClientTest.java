package io.vertx.axle.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import io.vertx.amqp.AmqpClientOptions;
import io.vertx.axle.amqp.AmqpClient;
import io.vertx.axle.amqp.AmqpMessage;
import io.vertx.axle.core.Vertx;

public class AMQPClientTest {

    @Rule
    public GenericContainer container = new GenericContainer("vromero/activemq-artemis")
            .withExposedPorts(5672);

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void testAxleAPI() {
        AmqpClientOptions options = new AmqpClientOptions()
                .setHost("localhost")
                .setPort(container.getMappedPort(5672))
                .setUsername("artemis")
                .setPassword("simetraehcapa");

        AmqpClient client = AmqpClient.create(vertx, options);
        PublisherBuilder<AmqpMessage> stream = client.createReceiver("my-address")
                .thenApply(receiver -> receiver.toPublisherBuilder()).toCompletableFuture().join();
        CompletionStage<Optional<String>> result = stream
                .map(AmqpMessage::bodyAsString)
                .findFirst().run();

        String payload = UUID.randomUUID().toString();
        client.createSender("my-address").thenApply(sender -> sender.write(AmqpMessage.create().withBody(payload).build()));

        Optional<String> optional = result.toCompletableFuture().join();
        assertThat(optional).contains(payload);

    }
}
