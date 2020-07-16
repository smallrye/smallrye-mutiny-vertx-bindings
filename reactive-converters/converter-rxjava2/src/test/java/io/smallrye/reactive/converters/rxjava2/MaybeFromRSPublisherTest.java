package io.smallrye.reactive.converters.rxjava2;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import io.reactivex.Maybe;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromRSPublisherTCK;

public class MaybeFromRSPublisherTest extends FromRSPublisherTCK<Maybe> {

    private ReactiveTypeConverter<Maybe> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Maybe.class)
                .orElseThrow(() -> new AssertionError("Maybe converter should be found"));
    }

    @Override
    protected ReactiveTypeConverter<Maybe> converter() {
        return converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String getOne(Maybe instance) {
        Maybe<String> maybe = instance.cast(String.class);
        return maybe.blockingGet();
    }

    @Override
    protected Exception getFailure(Maybe instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            //noinspection ResultOfMethodCallIgnored
            instance.blockingGet();
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getAll(Maybe instance) {
        Object val = instance.toSingle("DEFAULT").blockingGet();
        if (val.equals("DEFAULT")) {
            return Collections.emptyList();
        }
        return Collections.singletonList(val.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void consume(Maybe instance) {
        //noinspection ResultOfMethodCallIgnored,ConstantConditions
        instance.blockingGet(null);
    }
}
