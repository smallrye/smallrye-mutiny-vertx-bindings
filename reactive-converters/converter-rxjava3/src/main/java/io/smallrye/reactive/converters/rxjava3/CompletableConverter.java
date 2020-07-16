package io.smallrye.reactive.converters.rxjava3;

import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.reactive.converters.ReactiveTypeConverter;

/**
 * Converter handling the RX Java 3 {@link Completable} type.
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
        Completable s = Objects.requireNonNull(instance);
        return s.toCompletionStage(null);
    }

    @Override
    public Completable fromCompletionStage(CompletionStage cs) {
        return Completable.fromCompletionStage(cs)
                .onErrorResumeNext(t -> {
                    if (t instanceof CompletionException) {
                        return Completable.error(t.getCause());
                    }
                    return Completable.error(t);
                });
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
