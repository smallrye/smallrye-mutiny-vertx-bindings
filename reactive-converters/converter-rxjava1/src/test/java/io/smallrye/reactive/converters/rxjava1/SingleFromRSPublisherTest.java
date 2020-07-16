package io.smallrye.reactive.converters.rxjava1;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromRSPublisherTCK;
import rx.Single;

public class SingleFromRSPublisherTest extends FromRSPublisherTCK<Single> {

    private ReactiveTypeConverter<Single> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Single.class)
                .orElseThrow(() -> new AssertionError("Single converter should be found"));
    }

    @Override
    protected ReactiveTypeConverter<Single> converter() {
        return converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String getOne(Single instance) {
        Single<String> single = instance.cast(String.class);
        return single.toBlocking().value();
    }

    @Override
    protected Exception getFailure(Single instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            instance.toBlocking().value();
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }

    @Override
    protected List<String> getAll(Single instance) {
        String value = (String) instance.toBlocking().value();
        return Collections.singletonList(value);
    }

    @Override
    protected void consume(Single instance) {
        instance.toBlocking().value();
    }
}
