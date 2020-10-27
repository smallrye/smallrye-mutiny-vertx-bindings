package io.vertx.mutiny.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.rabbitmq.RabbitMQOptions;

public class RabbitMQClientTest {

    private static final String QUEUE = "my-queue";
    @Rule
    public GenericContainer<?> container = new FixedHostPortGenericContainer<>("rabbitmq:alpine")
            .withCreateContainerCmdModifier(cmd -> cmd.withHostName("my-rabbit"))
            .withExposedPorts(5672);

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
    public void testMutinyAPI()
            throws KeyManagementException, TimeoutException, NoSuchAlgorithmException, IOException, URISyntaxException {
        String uuid = UUID.randomUUID().toString();
        String uri = "amqp://" + container.getContainerIpAddress() + ":" + container.getMappedPort(5672);
        RabbitMQClient client = RabbitMQClient.create(vertx, new RabbitMQOptions()
                .setUri(uri));
        client.start().subscribeAsCompletionStage().join();
        createQueue(uri);
        RabbitMQConsumer consumer = client.basicConsumer(QUEUE).subscribeAsCompletionStage().join();
        Uni<String> stage = consumer.toMulti()
                .onItem().transform(m -> m.body().toString())
                .collectItems().first();

        client.basicPublish("", QUEUE, Buffer.buffer(uuid))
                .subscribeAsCompletionStage().join();

        Optional<String> object = stage.await().asOptional().indefinitely();
        assertThat(object).isNotEmpty().contains(uuid);

        client.stop().subscribeAsCompletionStage().join();
    }

    private void createQueue(String uri)
            throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(uri);
        try (Channel channel = factory.newConnection().createChannel()) {
            channel.queueDeclare(QUEUE, true, false, true, null);
        }
    }
}
