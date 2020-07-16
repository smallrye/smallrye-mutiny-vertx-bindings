package io.smallrye.reactive.converters.mutiny;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromCompletionStageTCK;

@SuppressWarnings("rawtypes")
public class MultiFromCompletionStageTest extends FromCompletionStageTCK<Multi> {

    private ReactiveTypeConverter<Multi> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Multi.class)
                .orElseThrow(() -> new AssertionError("Multi converter should be found"));
    }

    @Override
    protected ReactiveTypeConverter<Multi> converter() {
        return converter;
    }

    @Override
    protected String getOne(Multi instance) {
        return (String) instance.collectItems().first().await().indefinitely();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception getFailure(Multi instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            instance.subscribe().asIterable().forEach(x -> {

            });
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }
}
