package io.smallrye.mutiny.vertx.core;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

import io.vertx.core.impl.ContextInternal;
import io.vertx.mutiny.core.Context;

class ContextAwareExecutorWrapper implements ScheduledExecutorService {

    private final ScheduledExecutorService delegate;

    private final Supplier<Context> contextSupplier;

    ContextAwareExecutorWrapper(ScheduledExecutorService delegate, Supplier<Context> contextSupplier) {
        this.delegate = delegate;
        this.contextSupplier = contextSupplier;
    }

    private Runnable decorate(Runnable task) {
        return () -> {
            Context context = contextSupplier.get();
            ContextInternal contextInternal = (ContextInternal) context.getDelegate();
            if (!contextInternal.isDuplicate()) {
                context = Context.newInstance(contextInternal.duplicate());
            }
            context.runOnContext(task);
        };
    }

    // ---- ScheduledExecutorService ---- //

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return delegate.schedule(decorate(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return delegate.scheduleAtFixedRate(decorate(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return delegate.scheduleWithFixedDelay(decorate(command), initialDelay, delay, unit);
    }

    // ---- ExecutorService ---- //

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(decorate(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(decorate(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    // ---- Executor ---- //

    @Override
    public void execute(Runnable command) {
        delegate.execute(decorate(command));
    }
}
