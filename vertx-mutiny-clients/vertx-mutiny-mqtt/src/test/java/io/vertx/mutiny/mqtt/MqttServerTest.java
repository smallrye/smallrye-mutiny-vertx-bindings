package io.vertx.mutiny.mqtt;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.mqtt.MqttServerOptions;
import io.vertx.mutiny.core.Vertx;

class MqttServerTest {

    private Vertx vertx;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx, is(notNullValue()));
    }

    @AfterEach
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    void test() {
        MqttServer server = MqttServer.create(vertx, new MqttServerOptions()
                .setPort(0).setHost("0.0.0.0"))
                .endpointHandler(e -> {
                    // do nothing.
                })
                .listen().await().indefinitely();
        assertThat(server, is(notNullValue()));
        assertThat(server.actualPort(), is(not(0)));
    }

}
