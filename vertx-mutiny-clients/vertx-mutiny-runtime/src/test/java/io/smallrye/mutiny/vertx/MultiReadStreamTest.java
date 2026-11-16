package io.smallrye.mutiny.vertx;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

class MultiReadStreamTest {

    @Test
    void failureToRegisterHandlersIsSignaledDownstream() {
        Multi<String> multi = new MultiReadStream<>(new EndedReadStream(), s -> s);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        assertDoesNotThrow(() -> {
            Cancellable ignored = multi.subscribe().with(item -> fail("No item expected"), failure::set,
                    () -> fail("No completion expected"));
        });
        assertInstanceOf(IllegalStateException.class, failure.get());
        assertEquals("Stream already ended", failure.get().getMessage());
    }

    @Test
    void streamFailingWhileHandlersAreRegisteredYieldsSingleTerminalSignalAfterOnSubscribe() {
        Multi<String> multi = new MultiReadStream<>(new FailingWhileSubscribingReadStream(), s -> s);
        List<String> events = new CopyOnWriteArrayList<>();

        multi.subscribe().withSubscriber(new Flow.Subscriber<String>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                events.add("onSubscribe");
            }

            @Override
            public void onNext(String item) {
                events.add("onNext");
            }

            @Override
            public void onError(Throwable throwable) {
                events.add("onError: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                events.add("onComplete");
            }
        });

        // The original cause is kept, and Reactive Streams rules 1.7 and 1.9 are honoured
        assertEquals(List.of("onSubscribe", "onError: Connection reset"), events);
    }

    @Test
    void secondSubscriberIsRejectedAfterRegistrationFailure() {
        Multi<String> multi = new MultiReadStream<>(new EndedReadStream(), s -> s);
        AtomicReference<Throwable> first = new AtomicReference<>();
        AtomicReference<Throwable> second = new AtomicReference<>();

        multi.subscribe().with(item -> fail("No item expected"), first::set);
        multi.subscribe().with(item -> fail("No item expected"), second::set);

        assertEquals("Stream already ended", first.get().getMessage());
        assertEquals("This processor allows only a single Subscriber", second.get().getMessage());
    }

    @Test
    void noSignalIsDeliveredAfterCancellationInOnSubscribe() {
        Multi<String> multi = new MultiReadStream<>(new FailingWhileSubscribingReadStream(), s -> s);
        Recorder recorder = new Recorder(true);

        multi.subscribe().withSubscriber(recorder);

        assertEquals(List.of("onSubscribe"), recorder.events);
    }

    @Test
    void itemsAndCompletionAreForwarded() {
        ItemsReadStream stream = new ItemsReadStream("a", "b");
        Recorder recorder = new Recorder(false);

        new MultiReadStream<>(stream, String::toUpperCase).subscribe().withSubscriber(recorder);
        recorder.subscription.request(Long.MAX_VALUE);

        assertEquals(List.of("onSubscribe", "onNext: A", "onNext: B", "onComplete"), recorder.events);
        assertTrue(stream.released());
    }

    @Test
    void cancellationReleasesTheStream() {
        ItemsReadStream stream = new ItemsReadStream("a", "b");
        Recorder recorder = new Recorder(false);

        new MultiReadStream<>(stream, s -> s).subscribe().withSubscriber(recorder);
        recorder.subscription.request(1);
        recorder.subscription.cancel();

        assertEquals(List.of("onSubscribe", "onNext: a"), recorder.events);
        assertTrue(stream.released());
    }

    private static class Recorder implements Flow.Subscriber<String> {

        private final boolean cancelOnSubscribe;
        private final List<String> events = new CopyOnWriteArrayList<>();
        private Flow.Subscription subscription;

        Recorder(boolean cancelOnSubscribe) {
            this.cancelOnSubscribe = cancelOnSubscribe;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            events.add("onSubscribe");
            if (cancelOnSubscribe) {
                subscription.cancel();
            }
        }

        @Override
        public void onNext(String item) {
            events.add("onNext: " + item);
        }

        @Override
        public void onError(Throwable throwable) {
            events.add("onError: " + throwable.getMessage());
        }

        @Override
        public void onComplete() {
            events.add("onComplete");
        }
    }

    /**
     * Emits its items on demand, then ends.
     */
    private static class ItemsReadStream implements ReadStream<String> {

        private final List<String> items;
        private int next;
        private Handler<String> handler;
        private Handler<Void> endHandler;
        private Handler<Throwable> exceptionHandler;
        private boolean resumed;

        ItemsReadStream(String... items) {
            this.items = List.of(items);
        }

        boolean released() {
            return handler == null && endHandler == null && exceptionHandler == null && resumed;
        }

        @Override
        public ReadStream<String> exceptionHandler(Handler<Throwable> handler) {
            exceptionHandler = handler;
            return this;
        }

        @Override
        public ReadStream<String> handler(Handler<String> handler) {
            this.handler = handler;
            return this;
        }

        @Override
        public ReadStream<String> pause() {
            resumed = false;
            return this;
        }

        @Override
        public ReadStream<String> resume() {
            resumed = true;
            return this;
        }

        @Override
        public ReadStream<String> fetch(long amount) {
            for (long i = 0; i < amount && next < items.size() && handler != null; i++) {
                handler.handle(items.get(next++));
            }
            if (next == items.size() && endHandler != null) {
                endHandler.handle(null);
            }
            return this;
        }

        @Override
        public ReadStream<String> endHandler(Handler<Void> endHandler) {
            this.endHandler = endHandler;
            return this;
        }
    }

    /**
     * Mimics a stream failing concurrently with the handler registrations (e.g., a connection reset processed by the event
     * loop), after which it rejects handlers.
     */
    private static class FailingWhileSubscribingReadStream extends EndedReadStream {

        private Handler<Throwable> exceptionHandler;

        @Override
        public ReadStream<String> exceptionHandler(Handler<Throwable> handler) {
            exceptionHandler = handler;
            return this;
        }

        @Override
        public ReadStream<String> handler(Handler<String> handler) {
            if (handler != null && exceptionHandler != null) {
                exceptionHandler.handle(new IOException("Connection reset"));
            }
            return super.handler(handler);
        }

        @Override
        public ReadStream<String> endHandler(Handler<Void> endHandler) {
            return this;
        }
    }

    /**
     * Mimics a Vert.x stream that has already ended, such as {@code HttpClientResponseImpl} rejecting handlers.
     */
    private static class EndedReadStream implements ReadStream<String> {

        @Override
        public ReadStream<String> exceptionHandler(Handler<Throwable> handler) {
            return this;
        }

        @Override
        public ReadStream<String> handler(Handler<String> handler) {
            if (handler != null) {
                throw new IllegalStateException("Stream already ended");
            }
            return this;
        }

        @Override
        public ReadStream<String> pause() {
            return this;
        }

        @Override
        public ReadStream<String> resume() {
            return this;
        }

        @Override
        public ReadStream<String> fetch(long amount) {
            return this;
        }

        @Override
        public ReadStream<String> endHandler(Handler<Void> endHandler) {
            if (endHandler != null) {
                throw new IllegalStateException("Stream already ended");
            }
            return this;
        }
    }
}
