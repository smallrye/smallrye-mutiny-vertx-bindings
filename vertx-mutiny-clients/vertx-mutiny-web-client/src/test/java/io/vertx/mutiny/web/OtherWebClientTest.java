package io.vertx.mutiny.web;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.codec.BodyCodec;
import io.vertx.test.core.VertxTestBase;

public class OtherWebClientTest extends VertxTestBase {

    private Vertx vertx;
    private WebClient client;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        vertx = new Vertx(super.vertx);
    }

    @Test
    public void testGet() {
        int times = 5;
        waitFor(times);
        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8080));
        server.requestStream().handler(req -> req.response().setChunked(true).endAndForget("some_content"));
        try {
            server.listenAndAwait();
            client = WebClient.wrap(vertx.createHttpClient(new HttpClientOptions()));
            Uni<HttpResponse<Buffer>> uni = client
                    .get(8080, "localhost", "/the_uri")
                    .as(BodyCodec.buffer())
                    .send();
            for (int i = 0; i < times; i++) {
                uni.subscribe().with(resp -> {
                    Buffer body = resp.body();
                    assertEquals("some_content", body.toString("UTF-8"));
                    complete();
                }, this::fail);
            }
            await();
        } finally {
            server.closeAndAwait();
        }
    }

    @Test
    public void testPost() {
        int times = 5;
        waitFor(times);
        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8080));
        server.requestStream().handler(req -> req.bodyHandler(buff -> {
            assertEquals("onetwothree", buff.toString());
            req.response().endAndForget();
        }));
        try {
            server.listenAndAwait();
            client = WebClient.wrap(vertx.createHttpClient(new HttpClientOptions()));
            Multi<Buffer> stream = Multi.createFrom()
                    .items(Buffer.buffer("one"), Buffer.buffer("two"), Buffer.buffer("three"));
            Uni<HttpResponse<Buffer>> uni = client
                    .post(8080, "localhost", "/the_uri")
                    .sendStream(stream);
            for (int i = 0; i < times; i++) {
                uni.subscribe().with(resp -> complete(), this::fail);
            }
            await();
        } finally {
            server.closeAndAwait();
        }
    }

    @Test
    public void testResponseMissingBody() throws Exception {
        int times = 5;
        waitFor(times);
        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8080));
        server.requestStream().handler(req -> req.response().setStatusCode(403).endAndForget());
        try {
            server.listenAndAwait();
            client = WebClient.wrap(vertx.createHttpClient(new HttpClientOptions()));
            Uni<HttpResponse<Buffer>> uni = client
                    .get(8080, "localhost", "/the_uri")
                    .send();
            for (int i = 0; i < times; i++) {
                uni.subscribe().with(resp -> {
                    System.out.println("done");
                    assertEquals(403, resp.statusCode());
                    assertNull(resp.body());
                    complete();
                }, this::fail);
            }
            await();
        } finally {
            server.closeAndAwait();
        }
    }

    @Test
    public void testResponseBodyAsAsJsonMapped() throws Exception {
        JsonObject expected = new JsonObject().put("cheese", "Goat Cheese").put("wine", "Condrieu");
        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8080));
        server.requestStream().handler(req -> req.response().endAndForget(expected.encode()));
        try {
            server.listenAndAwait();
            client = WebClient.wrap(vertx.createHttpClient(new HttpClientOptions()));
            Uni<HttpResponse<WineAndCheese>> uni = client
                    .get(8080, "localhost", "/the_uri")
                    .as(BodyCodec.json(WineAndCheese.class))
                    .send();
            uni.subscribe().with(resp -> {
                assertEquals(200, resp.statusCode());
                assertEquals(new WineAndCheese().setCheese("Goat Cheese").setWine("Condrieu"), resp.body());
                testComplete();
            }, this::fail);
            await();
        } finally {
            server.closeAndAwait();
        }
    }

    @Test
    public void testErrorHandling() throws Exception {
        try {
            client = WebClient.wrap(vertx.createHttpClient(new HttpClientOptions()));
            Uni<HttpResponse<WineAndCheese>> uni = client
                    .get(-1, "localhost", "/the_uri")
                    .as(BodyCodec.json(WineAndCheese.class))
                    .send();
            uni.subscribe().with(resp -> fail(), error -> {
                assertEquals(IllegalArgumentException.class, error.getClass());
                testComplete();
            });
            await();
        } catch (Throwable t) {
            fail();
        }
    }
}
