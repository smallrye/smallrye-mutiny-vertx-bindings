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
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.handler.StaticHandler;

public class RouterTest {

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
    public void testRouter() throws InterruptedException {
        Router router = Router.router(vertx);
        router.get("/").handler(rc -> {
            rc.response().endAndForget("hello");
        });
        router.get("/assets/*").handler(rc -> StaticHandler.create("src/test/resources/assets").handle(rc));
        router.post().handler(rc -> BodyHandler.create().handle(rc));
        router.post("/post").handler(rc -> rc.response().endAndForget(rc.getBodyAsString()));

        vertx.createHttpServer()
                .requestHandler(router::handle)
                .listenAndAwait(8085);

        HttpClient client = vertx.createHttpClient();

        CountDownLatch latch1 = new CountDownLatch(1);
        HttpClientRequest request1 = client.getAbs("http://localhost:8085/");
        request1.toMulti().subscribe().with(
                resp -> resp.toMulti().subscribe().with(buffer -> {
                    assertEquals(buffer.toString(), "hello");
                    latch1.countDown();
                }));
        request1
                .exceptionHandler(t -> System.out.println("Got failure " + t))
                .endAndForget();
        assertTrue(latch1.await(1, TimeUnit.SECONDS));

        CountDownLatch latch2 = new CountDownLatch(1);
        HttpClientRequest request2 = client.getAbs("http://localhost:8085/assets/test.txt");
        request2.toMulti().subscribe().with(
                resp -> {
                    resp.toMulti().subscribe().with(buffer -> {
                        assertEquals(buffer.toString(), "This is a test.");
                        latch2.countDown();
                    });
                });
        request2
                .exceptionHandler(t -> System.out.println("Got failure " + t))
                .endAndForget();
        assertTrue(latch2.await(1, TimeUnit.SECONDS));

        CountDownLatch latch3 = new CountDownLatch(1);
        WebClient webClient = WebClient.create(vertx);
        HttpRequest<Buffer> request3 = webClient.postAbs("http://localhost:8085/post");
        Uni<HttpResponse<Buffer>> uni = request3
                .sendStream(Multi.createFrom().items("Hello", " ", "World", "!").map(Buffer::buffer));

        uni.subscribe().with(r -> {
            assertEquals(r.bodyAsString(), "Hello World!");
            latch3.countDown();
        });

        assertTrue(latch3.await(1, TimeUnit.SECONDS));
    }

}
