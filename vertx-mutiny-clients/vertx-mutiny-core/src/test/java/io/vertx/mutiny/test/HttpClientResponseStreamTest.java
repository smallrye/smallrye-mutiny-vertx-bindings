package io.vertx.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import io.vertx.mutiny.core.http.HttpServer;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/58401: the response body must not be lost when the request / send
 * / toMulti chain is subscribed from a non-Vert.x thread that gets delayed while subscribing.
 */
public class HttpClientResponseStreamTest {

    private static final Buffer BODY = Buffer.buffer("x".repeat(2500));

    private Vertx vertx;
    private HttpServer server;
    private HttpClient client;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer()
                .requestHandler(req -> req.response().endAndForget(BODY))
                .listenAndAwait(0);
        client = vertx.createHttpClient();
    }

    @AfterEach
    public void tearDown() {
        if (vertx != null) {
            vertx.closeAndAwait();
        }
    }

    @Test
    void responseBodyIsNotLostWhenSubscriberThreadIsDelayed() {
        RequestOptions options = new RequestOptions().setPort(server.actualPort()).setHost("localhost").setURI("/");

        // The chain is subscribed from the test thread, which has no Vert.x context.
        // The onSubscription hooks model that thread being delayed (e.g. descheduled) while subscribing; Vert.x threads are not delayed.
        List<String> threads = new CopyOnWriteArrayList<>();
        Uni<Buffer> body = client.request(options)
                .onSubscription().invoke(() -> delayIfNotOnVertxThread(() -> pause(Duration.ofMillis(500))))
                .onItem().transformToUni(req -> {
                    threads.add(Thread.currentThread().getName());
                    return req.send()
                            .onSubscription().invoke(() -> delayIfNotOnVertxThread(() -> waitForResponse(req)));
                })
                .onItem().transformToUni(resp -> {
                    threads.add(Thread.currentThread().getName());
                    return resp.toMulti().collect().in(Buffer::buffer, Buffer::appendBuffer);
                });

        assertThat(body.await().atMost(Duration.ofSeconds(5)).toString()).isEqualTo(BODY.toString());
        // send() and toMulti() are called on the event loop, so the response cannot be processed in between
        assertThat(threads).hasSize(2).allMatch(name -> name.startsWith("vert.x-eventloop-thread-"));
    }

    @Test
    void subscribingToAnEndedResponseFailsInsteadOfHanging() {
        RequestOptions options = new RequestOptions().setPort(server.actualPort()).setHost("localhost").setURI("/");

        // No handler is set while the response is received, so Vert.x drops the body and ends the response
        HttpClientResponse response = client.request(options)
                .chain(req -> req.send())
                .await().atMost(Duration.ofSeconds(5));
        response.end().await().atMost(Duration.ofSeconds(5));

        // Subscribed from an asynchronous step, as an exception thrown on the caller thread would surface anyway
        Uni<Buffer> body = response.end()
                .onItem().transformToUni(v -> response.toMulti().collect().in(Buffer::buffer, Buffer::appendBuffer));

        assertThatThrownBy(() -> body.await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Response already ended");
    }

    @Test
    void responsePausedInPipelineCanBeConsumedAfterAsynchronousSteps() {
        RequestOptions options = new RequestOptions().setPort(server.actualPort()).setHost("localhost").setURI("/");

        // Pattern documented in uni-and-multi.md, with the delay of the issue reproducer
        Buffer body = client.request(options)
                .onItem().transformToUni(request -> request.send())
                .onItem().invoke(HttpClientResponse::pause)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .onItem().delayIt().by(Duration.ofMillis(200))
                .onItem().transformToUni(response -> response.toMulti()
                        .collect().in(Buffer::buffer, Buffer::appendBuffer))
                .await().atMost(Duration.ofSeconds(5));

        assertThat(body.toString()).isEqualTo(BODY.toString());
    }

    @Test
    void responsePausedInPipelineCanBeConsumedLaterFromNonVertxThread() {
        RequestOptions options = new RequestOptions().setPort(server.actualPort()).setHost("localhost").setURI("/");

        // Pattern documented in uni-and-multi.md when blocking on a thread that is not a Vert.x thread
        HttpClientResponse response = client.request(options)
                .onItem().transformToUni(request -> request.send())
                .onItem().invoke(HttpClientResponse::pause)
                .await().atMost(Duration.ofSeconds(5));
        pause(Duration.ofMillis(200));
        Buffer body = response.toMulti()
                .collect().in(Buffer::buffer, Buffer::appendBuffer)
                .await().atMost(Duration.ofSeconds(5));

        assertThat(body.toString()).isEqualTo(BODY.toString());
    }

    @Test
    void responsePausedBeforeSendingFromNonVertxThreadCanBeConsumedLater() {
        RequestOptions options = new RequestOptions().setPort(server.actualPort()).setHost("localhost").setURI("/");

        // Pattern documented in uni-and-multi.md when send() is called on a thread that is not a Vert.x thread
        HttpClientRequest request = client.request(options).await().atMost(Duration.ofSeconds(5));
        request.getDelegate().response().onSuccess(io.vertx.core.http.HttpClientResponse::pause);
        HttpClientResponse response = request.send().await().atMost(Duration.ofSeconds(5));
        pause(Duration.ofMillis(200));
        Buffer body = response.toMulti()
                .collect().in(Buffer::buffer, Buffer::appendBuffer)
                .await().atMost(Duration.ofSeconds(5));

        assertThat(body.toString()).isEqualTo(BODY.toString());
    }

    private static void waitForResponse(HttpClientRequest req) {
        // Wait (bounded) until Vert.x has received the response, then leave it time to process the body
        long deadline = System.nanoTime() + Duration.ofMillis(500).toNanos();
        while (!req.getDelegate().response().isComplete() && System.nanoTime() < deadline) {
            pause(Duration.ofMillis(5));
        }
        pause(Duration.ofMillis(100));
    }

    private static void delayIfNotOnVertxThread(Runnable delay) {
        if (!io.vertx.core.Context.isOnVertxThread()) {
            delay.run();
        }
    }

    private static void pause(Duration duration) {
        LockSupport.parkNanos(duration.toNanos());
    }
}
