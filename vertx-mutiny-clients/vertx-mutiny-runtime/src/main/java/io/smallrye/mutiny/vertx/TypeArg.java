package io.smallrye.mutiny.vertx;

import java.lang.reflect.Field;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class TypeArg<T> {

    private static final TypeArg UNKNOWN = new TypeArg<>(Function.identity(), Function.identity());

    public static <U> TypeArg<U> of(Class<U> type) {
        MutinyGen gen = type.getAnnotation(MutinyGen.class);
        if (gen != null) {
            try {
                Field field = type.getField("__TYPE_ARG");
                return (TypeArg<U>) field.get(null);
            } catch (Exception ignore) {
            }
        }
        return unknown();
    }

    public static <T> TypeArg<T> unknown() {
        return (TypeArg<T>) UNKNOWN;
    }

    public final Function<Object, Object> wrap;
    public final Function<T, Object> unwrap;

    public TypeArg(Function<Object, Object> wrap, Function<T, Object> unwrap) {
        this.wrap = wrap;
        this.unwrap = unwrap;
    }

    public T wrap(Object o) {
        return o != null ? (T) wrap.apply(o) : null;
    }

    public <X> X unwrap(T o) {
        return o != null ? (X) unwrap.apply(o) : null;
    }
}
