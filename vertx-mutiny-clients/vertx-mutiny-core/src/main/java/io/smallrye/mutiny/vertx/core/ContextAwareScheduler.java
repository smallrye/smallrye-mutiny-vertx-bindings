package io.smallrye.mutiny.vertx.core;

import java.util.concurrent.*;

import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;

public class ContextAwareScheduler extends ScheduledThreadPoolExecutor {

    public ContextAwareScheduler(int corePoolSize) {
        super(corePoolSize);
    }

    public ContextAwareScheduler() {
        this(1);
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        return wrapWhenCallingFromVertxContext(task);
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
        return wrapWhenCallingFromVertxContext(task);
    }

    private <V> RunnableScheduledFuture<V> wrapWhenCallingFromVertxContext(RunnableScheduledFuture<V> task) {
        Context context = Vertx.currentContext();
        if (context != null) {
            return new Wrapper<>(task, context);
        } else {
            return task;
        }
    }

    private static class Wrapper<V> implements RunnableScheduledFuture<V> {
        private final RunnableScheduledFuture<V> task;
        private final Context context;

        private Wrapper(RunnableScheduledFuture<V> task, Context context) {
            this.task = task;
            this.context = context;
        }

        @Override
        public boolean isPeriodic() {
            return task.isPeriodic();
        }

        @Override
        public void run() {
            context.runOnContext(task::run);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return task.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public boolean isDone() {
            return task.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return task.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return task.get(timeout, unit);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return task.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed o) {
            return task.compareTo(o);
        }
    }
}
