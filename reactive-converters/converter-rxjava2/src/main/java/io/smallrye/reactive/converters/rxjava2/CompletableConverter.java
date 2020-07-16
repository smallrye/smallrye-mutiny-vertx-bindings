package io.smallrye.reactive.converters.rxjava2;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.smallrye.reactive.converters.ReactiveTypeConverter;

/**
 * Converter handling the RX Java 2 {@link Completable} type.
 *
 *
 * <strong>toCompletionStage</strong><br>
 * The {@link #toCompletionStage(Completable)} method returns a {@link CompletionStage} instance completed with
 * {@code null} upon success or failed according to the {@link Completable} signals.
 *
 *
 * <strong>fromCompletionStage</strong><br>
 * The {@link #fromCompletionStage(CompletionStage)} method returns a {@link Completable} instance completed or failed
 * according to the passed {@link CompletionStage} completion. If the future emits a {@code null} value, the
 * {@link Completable} is completed successfully. If the future redeems a non-null value, the {@link Completable}
 * completes successfully, but the value is ignored. If the future is completed with an exception, the
 * {@link Completable} fails.
 *
 *
 * <strong>fromPublisher</strong><br>
 * The {@link #fromPublisher(Publisher)} method returns a {@link Completable} emitting the completion signal when the
 * passed stream reached its end. If the passed {@link Publisher} is empty, the returned {@link Completable} completes.
 * If the passed stream emits values, they are discarded. If the passed @{link Publisher} emits a failure before its
 * completion, the returned {@link Completable} fails.
 *
 *
 * <strong>toRSPublisher</strong><br>
 * The {@link #toRSPublisher(Completable)} method returns a stream emitting an empty stream or a failed stream
 * depending of the {@link Completable}.
 *
 */
@SuppressWarnings("rawtypes")
public class CompletableConverter implements ReactiveTypeConverter<Completable> {

    @Override
    public <T> CompletionStage<T> toCompletionStage(Completable instance) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Completable s = Objects.requireNonNull(instance);
        //noinspection ResultOfMethodCallIgnored
        s.subscribe(
                () -> future.complete(null),
                future::completeExceptionally);
        return future;
    }

    @Override
    public Completable fromCompletionStage(CompletionStage cs) {
        CompletionStage<?> future = Objects.requireNonNull(cs);
        return Completable
                .create(emitter -> future.whenComplete((Object res, Throwable err) -> {
                    if (!emitter.isDisposed()) {
                        if (err != null) {
                            if (err instanceof CompletionException) {
                                emitter.onError(err.getCause());
                            } else {
                                emitter.onError(err);
                            }
                        } else {
                            emitter.onComplete();
                        }
                    }
                }));
    }

    @Override
    public <X> Publisher<X> toRSPublisher(Completable instance) {
        return instance.toFlowable();
    }

    @Override
    public <X> Completable fromPublisher(Publisher<X> publisher) {
        return Flowable.fromPublisher(publisher).ignoreElements();
    }

    @Override
    public Class<Completable> type() {
        return Completable.class;
    }

    @Override
    public boolean emitItems() {
        return false;
    }

    @Override
    public boolean emitAtMostOneItem() {
        return false;
    }

    @Override
    public boolean supportNullValue() {
        return false;
    }
}
