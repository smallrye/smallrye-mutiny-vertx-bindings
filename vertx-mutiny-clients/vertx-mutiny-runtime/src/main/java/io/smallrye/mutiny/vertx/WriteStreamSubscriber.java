package io.smallrye.mutiny.vertx;

import java.util.concurrent.Flow.Subscriber;
import java.util.function.Consumer;

/**
 * A {@link io.vertx.core.streams.WriteStream} to {@link Subscriber} adapter.
 *
 * @param <T> the type of item.
 */
@SuppressWarnings("SubscriberImplementation")
public interface WriteStreamSubscriber<T> extends Subscriber<T> {

    /**
     * Sets the handler to invoke on failure events.
     * <p>
     * The underlying {@link io.vertx.core.streams.WriteStream#end()} method is <strong>not</strong> invoked in this case.
     *
     * @param callback the callback invoked with the failure
     * @return a reference to this, so the API can be used fluently
     */
    WriteStreamSubscriber<T> onFailure(Consumer<? super Throwable> callback);

    /**
     * Sets the handler to invoke on completion events.
     * <p>
     * The underlying {@link io.vertx.core.streams.WriteStream#end()} method is invoked <strong>before</strong> the
     * given {@code callback}.
     *
     * @param callback the callback invoked when the completion event is received
     * @return a reference to this, so the API can be used fluently
     */
    WriteStreamSubscriber<T> onComplete(Runnable callback);

    /**
     * Sets the handler to invoke if the adapted {@link io.vertx.core.streams.WriteStream} fails.
     * <p>
     * The underlying {@link io.vertx.core.streams.WriteStream#end()} method is <strong>not</strong> invoked in this case.
     *
     * @param callback the callback invoked with the failure
     * @return a reference to this, so the API can be used fluently
     */
    WriteStreamSubscriber<T> onWriteStreamError(Consumer<? super Throwable> callback);
}
