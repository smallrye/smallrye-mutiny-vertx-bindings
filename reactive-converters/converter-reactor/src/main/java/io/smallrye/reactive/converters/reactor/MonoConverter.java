package io.smallrye.reactive.converters.reactor;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import reactor.core.publisher.Mono;

@SuppressWarnings("rawtypes")
public class MonoConverter implements ReactiveTypeConverter<Mono> {

    @SuppressWarnings("unchecked")
    @Override
    public <X> CompletionStage<X> toCompletionStage(Mono instance) {
        return instance.toFuture();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> Publisher<X> toRSPublisher(Mono instance) {
        return instance;
    }

    @Override
    public <X> Mono fromCompletionStage(CompletionStage<X> cs) {
        return Mono.create(sink -> cs.whenComplete((X v, Throwable e) -> {
            if (e != null) {
                sink.error(e instanceof CompletionException ? e.getCause() : e);
            } else if (v != null) {
                sink.success(v);
            } else {
                sink.success();
            }
        }));
    }

    @Override
    public <X> Mono fromPublisher(Publisher<X> publisher) {
        return Mono.from(publisher);
    }

    @Override
    public Class<Mono> type() {
        return Mono.class;
    }

    @Override
    public boolean emitItems() {
        return true;
    }

    @Override
    public boolean emitAtMostOneItem() {
        return true;
    }

    @Override
    public boolean supportNullValue() {
        return false;
    }
}
