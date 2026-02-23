package io.vertx.mutiny.test;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.core.VertxException;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.DeliveryContext;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import io.vertx.mutiny.core.eventbus.MessageConsumer;

public class EventbusTest {

    Vertx vertx;

    @AfterEach
    public void cleanup() {
        if (vertx != null) {
            vertx.closeAndAwait();
        }
    }

    @BeforeEach
    public void createVertx() {
        vertx = Vertx.vertx();
    }

    @Test
    public void testReply() {
        EventBus bus = vertx.eventBus();
        bus.consumer("address", message -> message.reply("world")).completionAndAwait();
        bus.request("address", "hello").await().atMost(Duration.of(10, ChronoUnit.SECONDS));
    }

    @Test
    public void testConversation() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        EventBus bus = vertx.eventBus();
        bus
                .consumer("address",
                        message -> message.replyAndRequest("world").subscribe().with(m -> latch.countDown()))
                .completionAndAwait();
        bus.request("address", "hello")
                .subscribe().with(m -> m.reply("done"), t -> {
                });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testSend() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        EventBus bus = vertx.eventBus();
        bus.consumer("address", message -> latch.countDown()).completionAndAwait();
        bus.send("address", "hello");
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testPublish() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        EventBus bus = vertx.eventBus();
        bus.consumer("address", message -> latch.countDown()).completionAndAwait();
        bus.consumer("address", message -> latch.countDown()).completionAndAwait();
        bus.publish("address", "hello");
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testConsumingAsMulti() {
        EventBus bus = vertx.eventBus();
        List<Integer> items = new ArrayList<>();
        bus.<Integer> consumer("address").toMulti()
                .onItem().transform(Message::body)
                .subscribe().with(items::add);

        bus.send("address", 1);
        bus.send("address", 2);
        bus.send("address", 3);
        bus.send("address", 4);

        await().until(() -> items.size() == 4);
    }

    @Test
    public void shouldRemoveInterceptor() {
        String headerName = UUID.randomUUID().toString();
        String headerValue = UUID.randomUUID().toString();
        Consumer<DeliveryContext<Object>> interceptor = dc -> {
            dc.message().headers().add(headerName, headerValue);
            dc.next();
        };
        EventBus eventBus = vertx.eventBus();
        eventBus.addInboundInterceptor(interceptor);

        MessageConsumer<Object> consumer = eventBus.consumer("foo", msg -> msg.reply(msg.headers().get(headerName)));
        Uni<Void> uni = consumer.completion()
                .chain(x -> eventBus.request("foo", "bar")
                        .chain(reply -> {
                            if (reply.body().equals(headerValue)) {
                                return Uni.createFrom().nullItem();
                            } else {
                                return Uni.createFrom().failure(VertxException.noStackTrace("Expected msg to be intercepted"));
                            }
                        }))
                .onItem().invoke(() -> eventBus.removeInboundInterceptor(interceptor))
                .chain(ignored -> eventBus.request("foo", "bar")
                        .chain(reply -> {
                            if (reply.body() == null) {
                                return Uni.createFrom().nullItem();
                            } else {
                                return Uni.createFrom()
                                        .failure(VertxException.noStackTrace("Expected msg not to be intercepted"));
                            }
                        }));
        uni.await().atMost(Duration.ofSeconds(100));
    }

}
