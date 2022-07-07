package io.vertx.mutiny.test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.DeliveryContext;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import io.vertx.mutiny.core.eventbus.MessageConsumer;
import io.vertx.test.core.VertxTestBase;

public class EventbusTest extends VertxTestBase {

    Vertx vertx;

    @After
    public void cleanup() {
        if (vertx != null) {
            vertx.closeAndAwait();
        }
    }

    @Before
    public void createVertx() {
        vertx = Vertx.vertx();
    }

    @Test
    public void testReply() {
        EventBus bus = vertx.eventBus();
        bus.consumer("address", message -> message.reply("world")).completionHandlerAndAwait();
        bus.request("address", "hello").subscribeAsCompletionStage().whenComplete((a, b) -> testComplete());
        await();
    }

    @Test
    public void testConversation() {
        EventBus bus = vertx.eventBus();
        bus
                .consumer("address",
                        message -> message.replyAndRequest("world").subscribe().with(m -> testComplete(), this::fail))
                .completionHandlerAndAwait();
        bus.request("address", "hello")
                .subscribe().with(m -> m.reply("done"), this::fail);
        await();
    }

    @Test
    public void testSend() {
        EventBus bus = vertx.eventBus();
        bus.consumer("address", message -> testComplete()).completionHandlerAndAwait();
        bus.send("address", "hello");
    }

    @Test
    public void testPublish() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        EventBus bus = vertx.eventBus();
        bus.consumer("address", message -> latch.countDown()).completionHandlerAndAwait();
        bus.consumer("address", message -> latch.countDown()).completionHandlerAndAwait();
        bus.publish("address", "hello");
        awaitLatch(latch);
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

        assertWaitUntil(() -> items.size() == 4);
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
        Uni<Void> uni = consumer.completionHandler()
                .chain(x -> eventBus.request("foo", "bar")
                        .chain(reply -> {
                            if (reply.body().equals(headerValue)) {
                                return Uni.createFrom().nullItem();
                            } else {
                                return Uni.createFrom().failure(new NoStackTraceThrowable("Expected msg to be intercepted"));
                            }
                        }))
                .onItem().invoke(() -> eventBus.removeInboundInterceptor(interceptor))
                .chain(ignored -> eventBus.request("foo", "bar")
                        .chain(reply -> {
                            if (reply.body() == null) {
                                return Uni.createFrom().nullItem();
                            } else {
                                return Uni.createFrom()
                                        .failure(new NoStackTraceThrowable("Expected msg not to be intercepted"));
                            }
                        }));
        uni.await().atMost(Duration.ofSeconds(100));
    }

}
