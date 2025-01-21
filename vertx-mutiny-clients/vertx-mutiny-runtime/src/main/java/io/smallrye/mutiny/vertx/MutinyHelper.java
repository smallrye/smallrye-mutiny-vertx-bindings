package io.smallrye.mutiny.vertx;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Consumer;
import java.util.function.Function;

import io.smallrye.mutiny.vertx.impl.WriteStreamSubscriberImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.streams.WriteStream;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class MutinyHelper {

    /**
     * Create an executor for a {@link io.vertx.core.Vertx} object, actions are executed on the event loop.
     *
     * @param vertx the vert.x object
     * @return the executor
     */
    public static Executor executor(io.vertx.core.Vertx vertx) {
        return command -> vertx.runOnContext(v -> command.run());
    }

    /**
     * Create an executor for a {@link io.vertx.core.Context}, actions are executed on the event loop of this
     * context.
     *
     * @param context the context object
     * @return the executor
     */
    public static Executor executor(io.vertx.core.Context context) {
        return command -> context.runOnContext(v -> command.run());

    }

    /**
     * Create an executor for a {@link io.vertx.core.Vertx} object, actions can be blocking, they are not executed
     * on Vert.x event loop.
     *
     * @param vertx the ver.tx object
     * @return the executor
     */
    public static Executor blockingExecutor(io.vertx.core.Vertx vertx) {
        return command -> vertx.executeBlocking(fut -> {
            command.run();
            fut.complete();
        }, null);
    }

    /**
     * Create an executor for a {@link io.vertx.core.Vertx} object, actions can be blocking, they are not executed
     * on Ver.tx event loop.
     *
     * @param vertx the vert.x object
     * @param ordered if true then if when tasks are scheduled several times on the same context, the executions
     *        for that context will be executed serially, not in parallel. if false then they will be no ordering
     *        guarantees
     * @return the executor
     */
    public static Executor blockingExecutor(Vertx vertx, boolean ordered) {
        return command -> vertx.executeBlocking(fut -> {
            command.run();
            fut.complete();
        }, ordered, null);
    }

    /**
     * Create a scheduler for a {@link io.vertx.core.WorkerExecutor} object, actions are executed on the threads of this
     * executor.
     *
     * @param worker the worker executor object
     * @return the executor
     */
    public static Executor blockingExecutor(WorkerExecutor worker) {
        return command -> worker.executeBlocking(fut -> {
            command.run();
            fut.complete();
        }, null);
    }

    /**
     * Unwrap the type used in Mutiny.
     *
     * @param type the type to unwrap
     * @return the unwrapped type
     */
    public static Class unwrap(Class<?> type) {
        if (type != null) {
            MutinyGen gen = type.getAnnotation(MutinyGen.class);
            if (gen != null) {
                return gen.value();
            }
        }
        return type;
    }

    /**
     * Convert a handler for a generated Mutiny type to a handler for the corresponding core type.
     */
    public static <BARE, MUTINY> Handler<BARE> convertHandler(Handler<MUTINY> mutinyHandler, Function<BARE, MUTINY> mapper) {
        if (mutinyHandler instanceof MutinyDelegate) {
            MutinyDelegate mutinyDelegate = (MutinyDelegate) mutinyHandler;
            return (Handler<BARE>) mutinyDelegate.getDelegate();
        }
        return new DelegatingHandler<>(mutinyHandler, mapper);
    }

    /**
     * Convert a consumer for a generated Mutiny type to a handler of the same type.
     */
    public static <T> Handler<T> convertConsumer(Consumer<T> mutinyConsumer) {
        if (mutinyConsumer instanceof MutinyDelegate) {
            return (Handler<T>) mutinyConsumer;
        }
        return mutinyConsumer != null ? new DelegatingConsumerHandler<>(mutinyConsumer) : null;
    }

    /**
     * Adapts a Vert.x {@link WriteStream} to a Mutiny {@link Subscriber}.
     * <p>
     * After subscription, the original {@link WriteStream} handlers should not be used anymore as they will be used by the
     * adapter.
     *
     * @param stream the stream to adapt
     * @return the adapted {@link Subscriber}
     */
    public static <T> WriteStreamSubscriber<T> toSubscriber(WriteStream<T> stream) {
        return toSubscriber(stream, Function.identity());
    }

    /**
     * Like {@link #toSubscriber(WriteStream)}, except the provided {@code mapping} function is applied to each item.
     */
    public static <R, T> WriteStreamSubscriber<R> toSubscriber(WriteStream<T> stream, Function<R, T> mapping) {
        return new WriteStreamSubscriberImpl<>(stream, mapping);
    }
}
