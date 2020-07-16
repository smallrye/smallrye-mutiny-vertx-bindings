package io.smallrye.reactive.converters;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.StreamSupport;

public class Registry {

    private static final List<ReactiveTypeConverter> converters;

    static {
        converters = new CopyOnWriteArrayList<>();
        // Load the converters from the service loader.
        StreamSupport.stream(ServiceLoader.load(ReactiveTypeConverter.class).spliterator(), false)
                .forEach(converters::add);
    }

    private Registry() {
        // Avoid direct instantiation.
    }

    public static <T> Optional<ReactiveTypeConverter<T>> lookup(Class<T> input) {
        return converters.stream().filter(entry -> entry.type().isAssignableFrom(Objects.requireNonNull(input)))
                .map(fc -> (ReactiveTypeConverter<T>) fc)
                .findAny();
    }

    public static void register(ReactiveTypeConverter... fcs) {
        Collections.addAll(converters, fcs);
    }

}
