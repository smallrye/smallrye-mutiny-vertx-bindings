package io.smallrye.mutiny.vertx.core;

import static io.smallrye.mutiny.vertx.core.Expectations.expectation;
import static io.vertx.core.http.HttpResponseExpectation.contentType;
import static io.vertx.core.http.HttpResponseExpectation.status;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.core.Expectation;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import io.vertx.mutiny.core.http.HttpServer;

public class ExpectationsTest {

    @Test
    public void plugMatchingExpectation() {
        Expectation<Integer> tenToTwenty = (value -> value >= 10 && value <= 20);

        UniAssertSubscriber<Integer> sub = Uni.createFrom().item(15)
                .plug(expectation(tenToTwenty))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        sub.assertItem(15);
    }

    @Test
    public void plugFailingExpectation() {
        Expectation<Integer> tenToTwenty = (value -> value >= 10 && value <= 20);

        UniAssertSubscriber<Integer> sub = Uni.createFrom().item(42)
                .plug(expectation(tenToTwenty))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        sub.assertFailedWith(VertxException.class, "Unexpected result: 42");
    }

    @Test
    public void httpAssertion() {
        Vertx vertx = Vertx.vertx();
        try {

            HttpServer server = vertx.createHttpServer()
                    .requestHandler(req -> req.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "text/plain")
                            .endAndForget("Yolo"))
                    .listen()
                    .await().atMost(Duration.ofSeconds(30));

            int port = server.actualPort();
            Buffer payload = vertx.createHttpClient()
                    .request(HttpMethod.GET, port, "localhost", "/")
                    .chain(HttpClientRequest::send)
                    .plug(expectation(HttpClientResponse::getDelegate, status(200).and(contentType("text/plain"))))
                    .onItem().transformToUni(HttpClientResponse::body)
                    .await().atMost(Duration.ofSeconds(5));
            assertThat(payload.toString(StandardCharsets.UTF_8)).isEqualTo("Yolo");

        } finally {
            vertx.closeAndAwait();
        }
    }
}
