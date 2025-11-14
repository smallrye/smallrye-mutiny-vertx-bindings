package io.vertx.mutiny.web;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.core.http.HttpServerRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.codec.BodyCodec;

class WebClientTest {

    Vertx vertx;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        vertx.closeAndAwait();
    }

    HttpServer startServer(Consumer<HttpServerRequest> handler) {
        return vertx.createHttpServer()
                .requestHandler(handler)
                .listenAndAwait(8081);
    }

    @Test
    public void getJsonObject() {
        HttpServer server = startServer(req -> {
            JsonObject data = new JsonObject().put("msg", "hello");
            req.response()
                    .putHeader("Content-Type", "application/json")
                    .endAndForget(data.encode());
        });

        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultPort(server.actualPort())
                .setDefaultHost("localhost"));

        HttpResponse<JsonObject> response = client.get("/yo")
                .as(BodyCodec.jsonObject())
                .sendAndAwait();

        assertThat(response.statusCode(), is(200));
        assertThat(response.getHeader("Content-Type"), is("application/json"));
        assertThat(response.body().getString("msg"), is("hello"));
    }

    @Test
    public void getJsonObjectMappedToRecord() {
        record Payload(String msg) {
        }

        HttpServer server = startServer(req -> {
            JsonObject data = new JsonObject().put("msg", "hello");
            req.response()
                    .putHeader("Content-Type", "application/json")
                    .endAndForget(data.encode());
        });

        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultPort(server.actualPort())
                .setDefaultHost("localhost"));

        HttpResponse<Payload> response = client.get("/yo")
                .as(BodyCodec.json(Payload.class))
                .sendAndAwait();

        assertThat(response.statusCode(), is(200));
        assertThat(response.getHeader("Content-Type"), is("application/json"));
        assertThat(response.body().msg(), is("hello"));
    }

    @Test
    public void sendJsonPost() {
        JsonObject outPayload = new JsonObject().put("foo", "bar");

        HttpServer server = startServer(req -> {
            req.bodyHandler(buffer -> req.response()
                    .putHeader("Content-Type", "application/json")
                    .endAndForget(buffer));
        });

        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultPort(server.actualPort())
                .setDefaultHost("localhost"));

        HttpResponse<JsonObject> response = client.post("/yo")
                .as(BodyCodec.jsonObject())
                .sendJsonObjectAndAwait(outPayload);

        assertThat(response.statusCode(), is(200));
        assertThat(response.getHeader("Content-Type"), is("application/json"));
        assertThat(response.body().getString("foo"), is("bar"));
    }
}
