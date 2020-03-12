package io.vertx.mutiny.web;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class WebClientTest {

    @Rule
    public GenericContainer container = new GenericContainer("kennethreitz/httpbin")
            .withExposedPorts(80)
            .withFileSystemBind("target", "/tmp/fakemail", BindMode.READ_WRITE);

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
    public void testWebClient() {
        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultPort(container.getMappedPort(80))
                .setDefaultHost(container.getContainerIpAddress()));
        assertThat(client, is(notNullValue()));

        JsonObject object = client.get("/get?msg=hello").send()
                .subscribeAsCompletionStage()
                .thenApply(HttpResponse::bodyAsJsonObject)
                .toCompletableFuture().join();
        assertThat(object.getJsonObject("args").getString("msg"), is("hello"));
    }
}
