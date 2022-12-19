package io.vertx.mutiny.amqp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.mutiny.core.Vertx;

public class AMQPClientTest {

    @Rule
    public GenericContainer<?> container = new GenericContainer<>("quay.io/artemiscloud/activemq-artemis-broker:1.0.11")
            .withEnv("AMQ_USER", "admin")
            .withEnv("AMQ_PASSWORD", "admin")
            .withEnv("AMQ_EXTRA_ARGS", "--nio")
            .withExposedPorts(5672);

    private Vertx vertx;

    @BeforeClass
    public static void beforeAll() {
        org.junit.Assume.assumeTrue(isNotArm64());

    }

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    static boolean isNotArm64() {
        String v = System.getProperty("os.arch");
        return !"aarch64".equalsIgnoreCase(v);
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testMutinyAPI() {
        String payload = UUID.randomUUID().toString();

        AmqpClientOptions options = new AmqpClientOptions()
                .setHost("localhost")
                .setPort(container.getMappedPort(5672))
                .setUsername("artemis")
                .setPassword("simetraehcapa");

        AmqpClient client = AmqpClient.create(vertx, options);
        Multi<AmqpMessage> stream = client.createReceiver("my-address")
                .onItem().transform(AmqpReceiver::toMulti)
                .await().indefinitely();

        Uni<AmqpMessage> first = stream.collect().first();

        client.createSender("my-address")
                .onItem().transformToUni(sender -> sender.write(AmqpMessage.create().withBody(payload).build()))
                .await().indefinitely();

        Optional<String> optional = first.map(AmqpMessage::bodyAsString).await().asOptional().indefinitely();
        assertThat(optional).contains(payload);

    }
}
