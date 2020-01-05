package io.vertx.mutiny.test;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.test.core.VertxTestBase;

public class EventbusTest extends VertxTestBase {

    @Test
    public void testReply() {
        Vertx vertx = Vertx.vertx();
        EventBus bus = vertx.eventBus();
        bus.consumer("address", message -> message.replyAndForget("world")).completionHandlerAndAwait();
        bus.request("address", "hello").subscribeAsCompletionStage().whenComplete((a, b) -> testComplete());
        await();
    }

    @Test
    public void testConversation() {
        Vertx vertx = Vertx.vertx();
        EventBus bus = vertx.eventBus();
        bus
                .consumer("address",
                        message -> message.replyAndRequest("world").subscribe().with(m -> testComplete(), this::fail))
                .completionHandlerAndAwait();
        bus.request("address", "hello")
                .subscribe().with(m -> m.replyAndForget("done"), this::fail);
        await();
    }

    @Test
    public void testSend() {
        Vertx vertx = Vertx.vertx();
        EventBus bus = vertx.eventBus();
        bus.consumer("address", message -> testComplete()).completionHandlerAndAwait();
        bus.sendAndForget("address", "hello");
    }

    @Test
    public void testPublish() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        Vertx vertx = Vertx.vertx();
        EventBus bus = vertx.eventBus();
        bus.consumer("address", message -> latch.countDown()).completionHandlerAndAwait();
        bus.consumer("address", message -> latch.countDown()).completionHandlerAndAwait();
        bus.publish("address", "hello");
        awaitLatch(latch);
    }

}
