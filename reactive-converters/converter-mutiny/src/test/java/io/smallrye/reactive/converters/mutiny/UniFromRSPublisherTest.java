package io.smallrye.reactive.converters.mutiny;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromRSPublisherTCK;

@SuppressWarnings("rawtypes")
public class UniFromRSPublisherTest extends FromRSPublisherTCK<Uni> {

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

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getAll(Uni instance) {
        return (List<String>) instance.await().asOptional().indefinitely()
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }

    @Override
    protected void consume(Uni instance) {
        instance.await().indefinitely();
    }

}
