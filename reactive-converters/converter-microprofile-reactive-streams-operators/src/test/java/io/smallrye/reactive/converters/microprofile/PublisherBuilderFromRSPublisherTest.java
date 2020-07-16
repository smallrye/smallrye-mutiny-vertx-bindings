package io.smallrye.reactive.converters.microprofile;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.junit.Before;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromRSPublisherTCK;

@SuppressWarnings("rawtypes")
public class PublisherBuilderFromRSPublisherTest extends FromRSPublisherTCK<PublisherBuilder> {

    @Before
    public void lookup() {
        converter = Registry.lookup(PublisherBuilder.class)
                .orElseThrow(() -> new AssertionError("PublisherBuilder converter should be found"));
    }

    private ReactiveTypeConverter<PublisherBuilder> converter;

    @Override
    protected ReactiveTypeConverter<PublisherBuilder> converter() {
        return converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String getOne(PublisherBuilder instance) {
        try {
            return ((Optional<String>) instance.findFirst().run().toCompletableFuture().join()).orElse(null);
        } catch (Exception e) {
            if (e instanceof CompletionException && e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw (RuntimeException) e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception getFailure(PublisherBuilder instance) {
        AtomicReference<Throwable> reference = new AtomicReference<>();
        try {
            instance.forEach(x -> {
                // Do nothing.
            }).run().toCompletableFuture().join();
        } catch (Exception e) {
            reference.set((e instanceof CompletionException ? e.getCause() : e));
        }
        return (Exception) reference.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getAll(PublisherBuilder instance) {
        try {
            return (List<String>) instance.toList().run().toCompletableFuture().join();
        } catch (Exception e) {
            if (e instanceof CompletionException && e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw (RuntimeException) e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void consume(PublisherBuilder instance) {
        instance.forEach(x -> {
            // Do nothing.
        }).run().toCompletableFuture().join();
    }

}
