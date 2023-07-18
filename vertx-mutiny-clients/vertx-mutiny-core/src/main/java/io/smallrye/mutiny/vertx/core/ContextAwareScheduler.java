package io.smallrye.mutiny.vertx.core;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.*;

import io.smallrye.common.annotation.CheckReturnValue;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;

/**
 * Helpers for producing Vert.x {@link Context}-aware schedulers.
 * <p>
 * The schedulers ensure that actions run on a Vert.x {@link Context} instead of the thread from the delegate
 * {@link ScheduledExecutorService}.
 * The {@link Context} are duplicated contexts.
 * <p>
 * The factory methods of {@link ContextCaptureStrategy} provide {@link ScheduledExecutorService} objects with limited
 * capabilities.
 * The methods that use {@link Runnable} tasks are supported, while those using {@link Callable} are not.
 * These executors cannot be shut down.
 */
public interface ContextAwareScheduler {

    /**
     * Define a scheduler {@link ScheduledExecutorService} delegate.
     *
     * @param delegate the delegate
     * @return an object to define the context capture strategy definition
     */
    @CheckReturnValue
    static ContextCaptureStrategy delegatingTo(ScheduledExecutorService delegate) {
        requireNonNull(delegate, "The delegate executor cannot be null");
        return new ContextCaptureStrategy(delegate);
    }

    /**
     * Define how a scheduler captures a Vert.x {@link Context}.
     */
    final class ContextCaptureStrategy {

        private final ScheduledExecutorService delegate;

        private ContextCaptureStrategy(ScheduledExecutorService delegate) {
            this.delegate = delegate;
        }

        /**
         * Actions will be run on an explicit {@link Context}.
         *
         * @param context the context, cannot be {@code null}
         * @return the scheduler
         */
        public ScheduledExecutorService withContext(Context context) {
            requireNonNull(context, "The context cannot be null");
            return new ContextAwareExecutorWrapper(delegate, () -> context);
        }

        /**
         * Actions will be run on a context that is captured by calling {@link Vertx#getOrCreateContext()}
         * when a scheduling method is being called like {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)}.
         *
         * @param vertx the Vert.x object, cannot be {@code null}
         * @return the scheduler
         */
        public ScheduledExecutorService withGetOrCreateContext(Vertx vertx) {
            requireNonNull(vertx, "The Vertx object cannot be null");
            return new ContextAwareExecutorWrapper(delegate, vertx::getOrCreateContext);
        }

        /**
         * Actions will be run on a context that is captured by calling {@link Vertx#getOrCreateContext()} on the
         * thread that is calling this method.
         * This is different to {@link #withGetOrCreateContext(Vertx)} which is late bound.
         *
         * @param vertx the Vert.x object, cannot be {@code null}
         * @return the scheduler
         */
        public ScheduledExecutorService withGetOrCreateContextOnCurrentThread(Vertx vertx) {
            requireNonNull(vertx, "The Vertx object cannot be null");
            return withContext(vertx.getOrCreateContext());
        }

        /**
         * Actions will be run on a context that is captured from the thread that is calling this method.
         * An exception is raised when the current thread is not a Vert.x thread and hence has no associated {@link Context}.
         *
         * @return the scheduler
         * @throws IllegalStateException when the current thread is not a Vert.x thread
         */
        public ScheduledExecutorService withCurrentContext() throws IllegalStateException {
            return withContext(captureCurrentContextOrFail());
        }

        private static Context captureCurrentContextOrFail() throws IllegalStateException {
            Context context = Vertx.currentContext();
            if (context == null) {
                throw new IllegalStateException("There is no Vert.x context in the current thread: " + Thread.currentThread());
            }
            return context;
        }
    }
}
