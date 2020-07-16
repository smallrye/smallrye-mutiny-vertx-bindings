package io.smallrye.reactive.converters.mutiny;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromRSPublisherTCK;

@SuppressWarnings("rawtypes")
public class MultiFromRSPublisherTest extends FromRSPublisherTCK<Multi> {

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
        return (String) instance.toUni().await().indefinitely();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception getFailure(Multi instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            instance.subscribe().asIterable().forEach(x -> {
                // do nothing
            });
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getAll(Multi instance) {
        return (List<String>) instance.collectItems().asList().await().indefinitely();
    }

    @Override
    protected void consume(Multi instance) {
        instance.collectItems().last().await().indefinitely();
    }
}
