package io.smallrye.reactive.converters.rxjava2;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import io.reactivex.Flowable;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromRSPublisherTCK;

public class FlowableFromRSPublisherTest extends FromRSPublisherTCK<Flowable> {

    private ReactiveTypeConverter<Flowable> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Flowable.class)
                .orElseThrow(() -> new AssertionError("Flowable converter should be found"));
    }

    @Override
    protected ReactiveTypeConverter<Flowable> converter() {
        return converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String getOne(Flowable instance) {
        Flowable<String> single = instance.cast(String.class);
        return single.blockingLast();
    }

    @Override
    protected Exception getFailure(Flowable instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            //noinspection ResultOfMethodCallIgnored
            instance.blockingLast();
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getAll(Flowable instance) {
        return ((Flowable<String>) instance).toList().blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void consume(Flowable instance) {
        //noinspection ResultOfMethodCallIgnored
        instance.forEach(x -> {
        });
    }
}
