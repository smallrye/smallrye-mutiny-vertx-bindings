package io.smallrye.reactive.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;

import org.junit.Test;
import org.reactivestreams.Publisher;

public class RegistryTest {

    @Test
    public void testConverterRegistrationAndLookup() {
        assertThat(Registry.lookup(CompletionStage.class)).isEmpty();
        Registry.register(new Myconverter());
        assertThat(Registry.lookup(CompletionStage.class)).isNotEmpty().containsInstanceOf(Myconverter.class);
    }

    @SuppressWarnings("rawtypes")
    private static class Myconverter implements ReactiveTypeConverter<CompletionStage> {

        @Override
        public <X> CompletionStage<X> toCompletionStage(CompletionStage instance) {
            return null;
        }

        @Override
        public CompletionStage fromCompletionStage(CompletionStage cs) {
            return cs;
        }

        @Override
        public <X> Publisher<X> toRSPublisher(CompletionStage instance) {
            return null;
        }

        @Override
        public <X> CompletionStage fromPublisher(Publisher<X> publisher) {
            return null;
        }

        @Override
        public Class<CompletionStage> type() {
            return CompletionStage.class;
        }

        @Override
        public boolean emitItems() {
            return false;
        }

        @Override
        public boolean emitAtMostOneItem() {
            return false;
        }

        @Override
        public boolean supportNullValue() {
            return false;
        }
    }

}
