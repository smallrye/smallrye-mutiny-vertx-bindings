package io.vertx.mutiny.http.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientAgent;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.httpproxy.HttpProxy;

public class HttpProxyTest {
    private static Vertx vertx;
    private static HttpProxy proxy;
    private static HttpServer originServer;
    private static HttpServer proxyServer;
    private static final String message = "<html><body><h1>I'm the target resource!</h1></body></html>";

    record Response(Buffer buff, Integer statusCode) {
    }

    @BeforeAll
    public static void setup() {
        vertx = Vertx.vertx();
        originServer = vertx.createHttpServer();

        originServer.requestHandler(req -> {
            req.response()
                    .putHeader("content-type", "text/html")
                    .endAndForget(message);
        }).listenAndAwait(8080);

        HttpClient proxyClient = vertx.createHttpClient();

        proxy = HttpProxy.reverseProxy(proxyClient);
        proxy.origin(8080, "localhost");

        proxyServer = vertx.createHttpServer();

        proxyServer.requestHandler(proxy).listenAndAwait(7070);
    }

    @Test
    public void testHttpProxy() throws InterruptedException {
        HttpClientAgent client = vertx.createHttpClient();
        var request = client.requestAndAwait(HttpMethod.GET, 7070, "localhost", "/");
        Response res = request.send()
                .flatMap(response -> response.body().map(buffer -> new Response(buffer, response.statusCode()))).await()
                .indefinitely();
        assertEquals(message, res.buff.toString());
        assertEquals(200, res.statusCode);
    }
}
