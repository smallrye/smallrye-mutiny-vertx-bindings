package io.smallrye.reactive.converters.reactor;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import reactor.core.publisher.Flux;

@SuppressWarnings("rawtypes")
public class FluxConverter implements ReactiveTypeConverter<Flux> {

    @SuppressWarnings("unchecked")
    @Override
    public <X> CompletionStage<X> toCompletionStage(Flux instance) {
        return instance.take(1).singleOrEmpty().toFuture();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> Publisher<X> toRSPublisher(Flux instance) {
        return instance;
    }

    @Override
    public <X> Flux fromCompletionStage(CompletionStage<X> cs) {
        return Flux.create(sink -> cs.whenComplete((X v, Throwable e) -> {
            if (e != null) {
                sink.error(e instanceof CompletionException ? e.getCause() : e);
            } else if (v != null) {
                sink.next(v);
                sink.complete();
            } else {
                sink.complete();
            }
        }));
    }

    @Override
    public <X> Flux fromPublisher(Publisher<X> publisher) {
        return Flux.from(publisher);
    }

    @Override
    public Class<Flux> type() {
        return Flux.class;
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
