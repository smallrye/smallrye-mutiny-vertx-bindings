package io.vertx.mutiny.web.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientAgent;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.proxy.handler.ProxyHandler;
import io.vertx.mutiny.httpproxy.HttpProxy;

public class WebProxyTest {
    private static Vertx vertx;
    private static HttpServer originServer;
    private static HttpServer proxyServer;
    private static final String message = "<html><body><h1>I'm the target resource!</h1></body></html>";

    record Response(Buffer buff, Integer statusCode) {
    }

    @BeforeAll
    public static void setup() {
        vertx = Vertx.vertx();
        originServer = vertx.createHttpServer();

        Router backendRouter = Router.router(vertx);
        backendRouter.route(HttpMethod.GET, "/foo").handler(rc -> {
            rc.response()
                    .putHeader("content-type", "text/html")
                    .endAndForget(message);
        });

        originServer.requestHandler(backendRouter).listenAndAwait(8080);

        proxyServer = vertx.createHttpServer();
        Router proxyRouter = Router.router(vertx);

        HttpClient proxyClient = vertx.createHttpClient();
        HttpProxy httpProxy = HttpProxy.reverseProxy(proxyClient);
        proxyRouter
                .route(HttpMethod.GET, "/foo")
                .handler(ProxyHandler.create(httpProxy, 8080, "localhost"));

        proxyServer.requestHandler(proxyRouter);
        proxyServer.listenAndAwait(7070);
    }

    @Test
    public void testHttpProxy() throws InterruptedException {
        HttpClientAgent client = vertx.createHttpClient();
        var request = client.requestAndAwait(HttpMethod.GET, 7070, "127.0.0.1", "/foo");
        Response response = request.send().flatMap(res -> res.body().map(buffer -> new Response(buffer, res.statusCode())))
                .await().atMost(Duration.ofSeconds(5));
        assertEquals(200, response.statusCode);
        assertEquals(message, response.buff.toString());
    }
}
