package io.smallrye.reactive.converters.microprofile;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.converters.ReactiveTypeConverter;

@SuppressWarnings("rawtypes")
public class PublisherBuilderConverter implements ReactiveTypeConverter<PublisherBuilder> {
    @SuppressWarnings("unchecked")
    @Override
    public <X> CompletionStage<X> toCompletionStage(PublisherBuilder instance) {
        return instance.findFirst().run().thenApplyAsync(x -> ((Optional) x).orElse(null));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> Publisher<X> toRSPublisher(PublisherBuilder instance) {
        return instance.buildRs();
    }

    @Override
    public <X> PublisherBuilder fromCompletionStage(CompletionStage<X> cs) {
        return ReactiveStreams.fromPublisher(Multi.createFrom().emitter(emitter -> cs.whenComplete((X v, Throwable e) -> {
            if (e != null) {
                emitter.fail(e instanceof CompletionException ? e.getCause() : e);
            } else if (v == null) {
                emitter.fail(new NullPointerException());
            } else {
                emitter.emit(v).complete();
            }
        })));
    }

    @Override
    public <X> PublisherBuilder fromPublisher(Publisher<X> publisher) {
        return ReactiveStreams.fromPublisher(publisher);
    }

    @Override
    public Class<PublisherBuilder> type() {
        return PublisherBuilder.class;
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
