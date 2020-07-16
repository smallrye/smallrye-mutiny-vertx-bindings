package io.smallrye.reactive.converters.reactor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromRSPublisherTCK;
import reactor.core.publisher.Flux;

public class FluxFromRSPublisherTest extends FromRSPublisherTCK<Flux> {

    private ReactiveTypeConverter<Flux> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Flux.class)
                .orElseThrow(() -> new AssertionError("Flux converter should be found"));
    }

    @Override
    protected ReactiveTypeConverter<Flux> converter() {
        return converter;
    }

    @Override
    protected String getOne(Flux instance) {
        return (String) instance.blockFirst();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception getFailure(Flux instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            instance.toIterable().forEach(x -> {
            });
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getAll(Flux instance) {
        return (List<String>) instance.collectList().block();
    }

    @Override
    protected void consume(Flux instance) {
        instance.last().block();
    }
}
