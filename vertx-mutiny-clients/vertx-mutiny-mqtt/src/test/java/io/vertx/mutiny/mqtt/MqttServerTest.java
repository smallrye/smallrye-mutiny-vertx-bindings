package io.vertx.mutiny.mqtt;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.mqtt.MqttServerOptions;
import io.vertx.mutiny.core.Vertx;

public class MqttServerTest {

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
