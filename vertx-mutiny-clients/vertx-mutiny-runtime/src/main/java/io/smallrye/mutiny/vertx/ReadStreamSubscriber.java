package io.smallrye.mutiny.vertx;

import java.util.ArrayDeque;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

public class ReadStreamSubscriber<R, J> implements Subscriber<R>, ReadStream<J> {

    private static final Runnable NOOP_ACTION = () -> {
    };
    private static final Throwable DONE_SENTINEL = new Throwable();

    public static final int BUFFER_SIZE = 16;

    public static <R, J> ReadStream<J> asReadStream(Publisher<R> multi, Function<R, J> adapter) {
        ReadStreamSubscriber<R, J> actual = new ReadStreamSubscriber<>(adapter);
        multi.subscribe(actual);
        return actual;
    }

    private final Function<R, J> adapter;
    private Handler<Void> endHandler;
    private Handler<Throwable> exceptionHandler;
    private Handler<J> elementHandler;
    private boolean paused = false;
    private Throwable completed;
    private ArrayDeque<R> pending = new ArrayDeque<>();
    private int requested = 0;
    private Subscription subscription;

    public ReadStreamSubscriber(Function<R, J> adapter) {
        this.adapter = adapter;
    }

    @Override
    public ReadStream<J> handler(Handler<J> handler) {
        synchronized (this) {
            elementHandler = handler;
        }
        checkStatus();
        return this;
    }

    @Override
    public ReadStream<J> pause() {
        synchronized (this) {
            paused = true;
        }
        return this;
    }

    @Override
    public ReadStream<J> fetch(long amount) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ReadStream<J> resume() {
        synchronized (this) {
            paused = false;
        }
        checkStatus();
        return this;
    }

    @Override
    public void onSubscribe(Subscription s) {
        synchronized (this) {
            subscription = s;
        }
        checkStatus();
    }

    private void checkStatus() {
        Runnable action = NOOP_ACTION;
        while (true) {
            J adapted;
            Handler<J> handler;
            synchronized (this) {
                if (!paused && (handler = elementHandler) != null && pending.size() > 0) {
                    requested--;
                    R item = pending.poll();
                    adapted = adapter.apply(item);
                } else {
                    if (completed != null) {
                        if (pending.isEmpty()) {
                            Handler<Throwable> onError;
                            Throwable result;
                            if (completed != DONE_SENTINEL) {
                                onError = exceptionHandler;
                                result = completed;
                                exceptionHandler = null;
                            } else {
                                onError = null;
                                result = null;
                            }
                            Handler<Void> onCompleted = endHandler;
                            endHandler = null;
                            action = () -> {
                                try {
                                    if (onError != null) {
                                        onError.handle(result);
                                    }
                                } finally {
                                    if (onCompleted != null) {
                                        onCompleted.handle(null);
                                    }
                                }
                            };
                        }
                    } else if (elementHandler != null && requested < BUFFER_SIZE / 2) {
                        int request = BUFFER_SIZE - requested;
                        action = () -> subscription.request(request);
                        requested = BUFFER_SIZE;
                    }
                    break;
                }
            }
            handler.handle(adapted);
        }
        action.run();
    }

    @Override
    public ReadStream<J> endHandler(Handler<Void> handler) {
        synchronized (this) {
            if (completed == null || pending.size() > 0) {
                endHandler = handler;
            } else {
                if (handler != null) {
                    throw new IllegalStateException();
                }
            }
        }
        return this;
    }

    @Override
    public ReadStream<J> exceptionHandler(Handler<Throwable> handler) {
        synchronized (this) {
            if (completed == null || pending.size() > 0) {
                exceptionHandler = handler;
            } else {
                if (handler != null) {
                    throw new IllegalStateException();
                }
            }
        }
        return this;
    }

    @Override
    public void onComplete() {
        onError(DONE_SENTINEL);
    }

    @Override
    public void onError(Throwable e) {
        synchronized (this) {
            if (completed != null) {
                return;
            }
            completed = e;
        }
        checkStatus();
    }

    @Override
    public void onNext(R item) {
        synchronized (this) {
            pending.add(item);
        }
        checkStatus();
    }
}
