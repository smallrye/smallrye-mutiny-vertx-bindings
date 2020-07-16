package io.smallrye.reactive.converters.rxjava2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.smallrye.reactive.converters.ReactiveTypeConverter;

/**
 * Converter handling the RX Java 2 {@link Observable} type.
 *
 *
 * <strong>toCompletionStage</strong><br>
 * The {@link #toCompletionStage(Observable)} method returns a {@link CompletionStage} instance completed or failed
 * according to the stream emissions. The returned {@link CompletionStage} is redeemed either the first emitted value or
 * {@code null} to distinguish stream emitting values from empty streams. If the stream is empty, the returned
 * {@link CompletionStage} is completed with {@code null}. If the stream emits multiple values, the first one is
 * used, and the {@link CompletionStage} is completed with an instance of the first emitted item. Other items and
 * potential error are ignored. If the stream fails before emitting a first item, the {@link CompletionStage} is
 * completed with the failure.
 *
 *
 * <strong>fromCompletionStage</strong><br>
 * The {@link #fromCompletionStage(CompletionStage)} method returns a {@link Observable} instance completed or failed
 * according to the passed {@link CompletionStage} completion. Note that if the future emits a {@code null} value,
 * the {@link Observable} fails. If the future completes with a value, the observable emits the value and then completes.
 * If the future completes with a failure, the stream emits the failure.
 *
 *
 * <strong>fromPublisher</strong><br>
 * The {@link #fromPublisher(Publisher)} method returns a {@link Observable} emitting the same items, failure and
 * completion as the passed {@link Publisher}. If the passed {@link Publisher} is empty, the returned {@link Observable}
 * is also empty. The source {@link Publisher} is consumed in an unbounded fashion without applying any back-pressure to
 * it. This is because of {@link Observable#fromPublisher(Publisher)} used by this method.
 *
 *
 * <strong>toRSPublisher</strong><br>
 * The {@link #toRSPublisher(Observable)} method returns a {@link Publisher} emitting the same events as the source
 * {@link Observable}. This operations applies the a {@code missing} back-pressure strategy. {@code OnNext} events are
 * written without any buffering or dropping. The consumer of the returned {@link Publisher} has to deal with any
 * overflow.
 *
 */
@SuppressWarnings("rawtypes")
public class ObservableConverter implements ReactiveTypeConverter<Observable> {

    static <X> void toStreamEvents(CompletionStage<X> cs, Emitter<Object> emitter) {
        cs.whenComplete((X res, Throwable err) -> {
            if (res != null) {
                emitter.onNext(res);
                emitter.onComplete();
            } else {
                if (err != null) {
                    emitter.onError(err instanceof CompletionException ? err.getCause() : err);
                } else {
                    emitter.onComplete();
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Publisher<T> toRSPublisher(Observable instance) {
        return instance.toFlowable(BackpressureStrategy.MISSING);
    }

    @Override
    public Observable fromPublisher(Publisher publisher) {
        return Observable.fromPublisher(publisher);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CompletionStage<T> toCompletionStage(Observable instance) {
        CompletableFuture<T> future = new CompletableFuture<>();
        //noinspection ResultOfMethodCallIgnored
        ((Observable<T>) instance).firstElement().subscribe(
                future::complete,
                future::completeExceptionally,
                () -> future.complete(null));
        return future;
    }

    @Override
    public <X> Observable fromCompletionStage(CompletionStage<X> cs) {
        return Observable.create(emitter -> toStreamEvents(cs, emitter));
    }

    @Override
    public Class<Observable> type() {
        return Observable.class;
    }

    @Override
    public boolean emitItems() {
        return true;
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
