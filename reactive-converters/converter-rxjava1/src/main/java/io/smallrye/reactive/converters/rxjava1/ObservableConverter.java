package io.smallrye.reactive.converters.rxjava1;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import rx.Emitter;
import rx.Observable;

/**
 * Converter handling the RX Java {@link Observable} type.
 *
 *
 * <strong>toCompletionStage</strong><br>
 * The {@link #toCompletionStage(Observable)} method returns a {@link CompletionStage} instance completed or failed
 * according to the stream emissions. The returned {@link CompletionStage} is redeemed either the first emitted value or
 * {@code null} to distinguish stream emitting values from empty streams. If the stream is empty, the returned
 * {@link CompletionStage} is completed with {@code null}. If the stream emits {@code null} as first vale, the
 * {@link CompletionStage} is also completed with {@code null}. If the stream emits multiple values, the first one is
 * used, and the {@link CompletionStage} is completed with an instance of the first emitted item. Other items and
 * potential error are ignored. If the stream fails before emitting a first item, the {@link CompletionStage} is
 * completed with the failure.
 *
 * <strong>fromCompletionStage</strong><br>
 * The {@link #fromCompletionStage(CompletionStage)} method returns a {@link Observable} instance completed or failed
 * according to the passed {@link CompletionStage} completion. Note that if the future emits a {@code null} value,
 * the {@link Observable} emits {@code null}. If the future completes with a value, the observable emits the value and
 * then completes. If the future completes with a failure, the stream emits the failure.
 *
 *
 * <strong>fromPublisher</strong><br>
 * The {@link #fromPublisher(Publisher)} method returns a {@link Observable} emitting the same items, failure and
 * completion as the passed {@link Publisher}. If the passed {@link Publisher} is empty, the returned {@link Observable}
 * is also empty. The operation doesn't interfere with back-pressure which is determined by the source
 * {@code Publisher}'s back-pressure behavior.
 *
 *
 * <strong>toRSPublisher</strong><br>
 * The {@link #toRSPublisher(Observable)} method returns a {@link Publisher} emitting the same events as the source
 * {@link Observable}. The operation doesn't interfere with back-pressure which is determined by the source
 * {@code Observable}'s back-pressure behavior. If the passed {@link Observable} emits a {@code null} value, the
 * returned {@link Publisher} fails.
 *
 */
@SuppressWarnings("rawtypes")
public class ObservableConverter implements ReactiveTypeConverter<Observable> {

    private static <X> void toStreamEvents(CompletionStage<X> cs, Emitter<Object> emitter) {
        cs.whenComplete((X res, Throwable err) -> {
            if (err != null) {
                if (err instanceof CompletionException) {
                    emitter.onError(err.getCause());
                } else {
                    emitter.onError(err);
                }
            } else {
                emitter.onNext(res);
                emitter.onCompleted();
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Publisher<T> toRSPublisher(Observable instance) {
        return RxJavaInterop.toV2Flowable(instance);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable fromPublisher(Publisher publisher) {
        return RxJavaInterop.toV1Observable(publisher);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CompletionStage<T> toCompletionStage(Observable instance) {
        CompletableFuture<T> future = new CompletableFuture<>();
        ((Observable<T>) instance)
                .firstOrDefault(null)
                .subscribe(
                        future::complete,
                        future::completeExceptionally);
        return future;
    }

    @Override
    public <X> Observable fromCompletionStage(CompletionStage<X> cs) {
        return Observable.create(emitter -> toStreamEvents(cs, emitter), Emitter.BackpressureMode.ERROR);
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
        return true;
    }
}
