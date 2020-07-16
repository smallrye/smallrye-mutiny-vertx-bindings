package io.smallrye.reactive.converters.mutiny;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromCompletionStageTCK;

@SuppressWarnings("rawtypes")
public class UniFromCompletionStageTest extends FromCompletionStageTCK<Uni> {

    private ReactiveTypeConverter<Uni> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Uni.class)
                .orElseThrow(() -> new AssertionError("Uni converter should be found"));
    }

    @Override
    protected ReactiveTypeConverter<Uni> converter() {
        return converter;
    }

    @Override
    protected String getOne(Uni instance) {
        return (String) instance.await().indefinitely();
    }

    @Override
    protected Exception getFailure(Uni instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            instance.await().indefinitely();
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }
}
