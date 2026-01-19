import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.shell.term.HttpTermOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import io.vertx.mutiny.ext.shell.term.TermServer;

class SmokeTest {

    @Test
    void checkTermServer() {
        Vertx vertx = Vertx.vertx();
        try {
            TermServer server = TermServer.createHttpTermServer(vertx,
                    new HttpTermOptions().setPort(5000).setHost("localhost"));
            server.termHandler(term -> term.stdinHandler(term::write));
            server.listenAndAwait();

            HttpClientResponse response = vertx.createHttpClient()
                    .request(HttpMethod.GET, 5000, "localhost", "/shell.html")
                    .flatMap(HttpClientRequest::send)
                    .await().atMost(Duration.ofSeconds(5));

            assertEquals(200, response.statusCode());
        } finally {
            vertx.closeAndAwait();
        }
    }
}
