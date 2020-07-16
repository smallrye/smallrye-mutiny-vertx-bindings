package io.smallrye.reactive.converters.reactor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromRSPublisherTCK;
import reactor.core.publisher.Mono;

public class MonoFromRSPublisherTest extends FromRSPublisherTCK<Mono> {

    private ReactiveTypeConverter<Mono> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Mono.class)
                .orElseThrow(() -> new AssertionError("Mono converter should be found"));
    }

    @Override
    protected ReactiveTypeConverter<Mono> converter() {
        return converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String getOne(Mono instance) {
        return (String) instance.blockOptional().orElse(null);
    }

    @Override
    protected Exception getFailure(Mono instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            instance.block();
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getAll(Mono instance) {
        return (List<String>) instance.blockOptional()
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }

    @Override
    protected void consume(Mono instance) {
        instance.block();
    }

}
