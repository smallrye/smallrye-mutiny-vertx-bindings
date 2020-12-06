package io.vertx.mutiny.micrometer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.client.WebClient;

public class MicrometerMetricsTest {

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
        Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
                new MicrometerMetricsOptions()
                        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
                        .setEnabled(true)));

        Router router = Router.router(vertx);
        router.route("/metrics").handler(x -> PrometheusScrapingHandler.create().handle(x));
        HttpServer server = vertx.createHttpServer().requestHandler(router).listenAndAwait(8080);

        WebClient client = WebClient.create(vertx, new WebClientOptions().setDefaultPort(8080));
        String s = client.get("/metrics").sendAndAwait().bodyAsString();
        assertThat(s).contains("vertx_http_client_active_connections");

        MetricsService metricsService = MetricsService.create(server);
        JsonObject metrics = metricsService.getMetricsSnapshot();
        System.out.println(metrics);
        assertThat(metrics.getJsonArray("vertx.http.server.active.connections")).isNotNull();

    }

}
