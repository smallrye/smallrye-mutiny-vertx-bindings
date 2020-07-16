package io.vertx.mutiny.mqtt;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.mqtt.messages.MqttConnAckMessage;

public class MqttClientTest {

    @ClassRule
    public static GenericContainer<?> mosquitto = new GenericContainer<>("eclipse-mosquitto:latest")
            .withExposedPorts(1883)
            .waitingFor(Wait.forLogMessage(".*listen socket on port 1883.*\\n", 2));
    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx, is(notNullValue()));
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void test() {
        MqttConnAckMessage connection = MqttClient.create(vertx)
                .connect(mosquitto.getMappedPort(1883), mosquitto.getContainerIpAddress(), null)
                .await().indefinitely();
        assertThat(connection, is(notNullValue()));
    }

}
