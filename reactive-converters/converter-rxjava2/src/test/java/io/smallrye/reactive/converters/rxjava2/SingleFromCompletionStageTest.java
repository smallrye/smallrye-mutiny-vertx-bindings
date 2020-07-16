package io.smallrye.reactive.converters.rxjava2;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;

import io.reactivex.Single;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromCompletionStageTCK;

public class SingleFromCompletionStageTest extends FromCompletionStageTCK<Single> {

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
        return single.blockingGet();
    }

    @Override
    protected Exception getFailure(Single instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            //noinspection ResultOfMethodCallIgnored
            instance.blockingGet();
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }
}
