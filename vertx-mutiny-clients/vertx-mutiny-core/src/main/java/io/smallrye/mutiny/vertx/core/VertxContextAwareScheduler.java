package io.smallrye.mutiny.vertx.core;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * A helper for the scheduled tasks methods in {@link ScheduledExecutorService} that supports Vert.x contexts.
 * <p>
 * This {@link ScheduledExecutorService} delegates to a concrete {@link ScheduledExecutorService}, but the following
 * scheduled tasks get executed on a Vert.x {@link Context}, if available:
 * <ul>
 * <li>{@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)}</li>
 * <li>{@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}</li>
 * <li>{@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}</li>
 * </ul>
 * <p>
 * {@link ScheduledExecutorService#schedule(Callable, long, TimeUnit)} throws a {@link UnsupportedOperationException}.
 * <p>
 * The other methods from {@link ExecutorService} delegate to the provided {@link ScheduledExecutorService} and run on
 * one of its managed thread.
 * <p>
 * When a task is scheduled with one of the 3 methods above (@{code schedule, scheduleAtFixedRate, scheduleWithFixedDelay})
 * but the call is not from a Vert.x {@link Context}, then the task is run on a managed thread of the provided
 * {@link ScheduledExecutorService}.
 */
public class VertxContextAwareScheduler implements ScheduledExecutorService {

    /**
     * Create a new {@link VertxContextAwareScheduler} from a delegate {@link ScheduledExecutorService}.
     *
     * @param delegate the delegate executor, must not be {@code null}
     * @return the scheduler
     */
    public static VertxContextAwareScheduler wrapping(ScheduledExecutorService delegate) {
        return new VertxContextAwareScheduler(requireNonNull(delegate, "The delegate executor cannot be null"));
    }

    /**
     * Create a new {@link VertxContextAwareScheduler} that delegates to the Mutiny worker pool.
     *
     * @return the scheduler
     * @see Infrastructure#getDefaultWorkerPool()
     */
    public static VertxContextAwareScheduler wrappingMutinyWorkerPool() {
        return new VertxContextAwareScheduler(Infrastructure.getDefaultWorkerPool());
    }

    private final ScheduledExecutorService delegate;

    private VertxContextAwareScheduler(ScheduledExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }

    // ---- Scheduled tasks that can possibly be run back on a Vert.x context ---- //

    private Runnable wrap(Runnable command) {
        Context context = Vertx.currentContext();
        if (context == null) {
            return command;
        }
        return () -> context.runOnContext(v -> command.run());
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return delegate.schedule(wrap(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return delegate.scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return delegate.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
    }
}
