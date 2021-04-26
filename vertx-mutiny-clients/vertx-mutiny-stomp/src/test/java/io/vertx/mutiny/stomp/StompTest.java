package io.vertx.mutiny.stomp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.ext.stomp.StompClientOptions;
import io.vertx.ext.stomp.StompServerOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.stomp.StompClient;
import io.vertx.mutiny.ext.stomp.StompServer;
import io.vertx.mutiny.ext.stomp.StompServerHandler;

public class StompTest {

    private static final int PORT = 62613;
    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void test() {
        CompletableFuture<String> future = new CompletableFuture<>();
        StompServerHandler handler = StompServerHandler.create(vertx);
        StompServer server = StompServer.create(vertx, new StompServerOptions().setPort(PORT)).handler(handler)
                .listenAndAwait();

        StompClient client = StompClient.create(vertx, new StompClientOptions().setPort(PORT));
        client
                .connectAndAwait()
                .subscribeAndAwait("/q", frame -> future.complete(frame.getBodyAsString()));

        StompClient client2 = StompClient.create(vertx, new StompClientOptions().setPort(PORT));
        client2
                .connectAndAwait()
                .sendAndAwait("/q", Buffer.buffer("hello"));

        String s = future.join();
        assertThat(s).isEqualTo("hello");
        client.close();
        server.closeAndAwait();
    }

}
