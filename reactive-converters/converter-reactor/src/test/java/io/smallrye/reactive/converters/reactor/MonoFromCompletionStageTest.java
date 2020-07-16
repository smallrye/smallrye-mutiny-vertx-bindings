package io.smallrye.reactive.converters.reactor;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromCompletionStageTCK;
import reactor.core.publisher.Mono;

public class MonoFromCompletionStageTest extends FromCompletionStageTCK<Mono> {

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

    @Override
    protected String getOne(Mono instance) {
        return (String) instance.toFuture().join();
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
}
