package io.vertx.mutiny.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.handler.StaticHandler;
import io.vertx.mutiny.mutiny.web.TestRouteHandler;

public class RouterTest {

    private Vertx vertx;
    private Router router;
    private HttpServer server;
    private HttpClient client;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        router = Router.router(vertx);
        server = vertx.createHttpServer()
                .requestHandler(router)
                .listenAndAwait(0, "localhost");
        client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(server.actualPort()));
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close().onFailure().recoverWithNull().await().indefinitely();
        }
        if (server != null) {
            server.close().onFailure().recoverWithNull().await().indefinitely();
        }
        vertx.closeAndAwait();
    }

    @Test
    public void testRouter() throws InterruptedException {
        router.get("/").handler(rc -> {
            rc.response().endAndForget("hello");
        });
        router.get("/assets/*").handler(StaticHandler.create("src/test/resources/assets"));
        router.post().handler(BodyHandler.create());
        router.post("/post").handler(rc -> rc.response().endAndForget(rc.body().asString()));

        CountDownLatch latch1 = new CountDownLatch(1);
        RequestOptions req1 = new RequestOptions()
                .setAbsoluteURI(String.format("http://localhost:%d", server.actualPort()));
        HttpClientRequest request1 = client.requestAndAwait(req1);
        request1.response().subscribe().with(
                resp -> resp.toMulti().subscribe().with(buffer -> {
                    assertEquals("hello", buffer.toString());
                    latch1.countDown();
                }));
        request1
                .exceptionHandler(t -> System.out.println("Got failure " + t))
                .endAndForget();
        assertTrue(latch1.await(1, TimeUnit.SECONDS));

        CountDownLatch latch2 = new CountDownLatch(1);
        RequestOptions req2 = new RequestOptions()
                .setAbsoluteURI(String.format("http://localhost:%d/assets/test.txt", server.actualPort()));
        HttpClientRequest request2 = client.requestAndAwait(req2);
        request2.response().subscribe().with(
                resp -> {
                    resp.toMulti().subscribe().with(buffer -> {
                        assertEquals("This is a test.", buffer.toString());
                        latch2.countDown();
                    });
                });
        request2
                .exceptionHandler(t -> System.out.println("Got failure " + t))
                .endAndForget();
        assertTrue(latch2.await(1, TimeUnit.SECONDS));

        CountDownLatch latch3 = new CountDownLatch(1);
        WebClient webClient = WebClient.wrap(client);
        HttpRequest<Buffer> request3 = webClient.postAbs(String.format("http://localhost:%d/post", server.actualPort()));
        Uni<HttpResponse<Buffer>> uni = request3
                .sendStream(Multi.createFrom().items("Hello", " ", "World", "!").map(Buffer::buffer));

        uni.subscribe().with(r -> {
            assertEquals("Hello World!", r.bodyAsString());
            latch3.countDown();
        });

        assertTrue(latch3.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testOrderListenerIsInvoked() {
        router.get().handler(TestRouteHandler.create());
        WebClient webClient = WebClient.wrap(client);
        int statusCode = webClient.get("/").sendAndAwait().statusCode();
        assertEquals(200, statusCode);
    }
}
