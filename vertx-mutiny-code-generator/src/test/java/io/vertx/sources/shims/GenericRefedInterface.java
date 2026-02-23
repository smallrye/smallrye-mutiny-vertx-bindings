package io.vertx.sources.shims;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public interface GenericRefedInterface<T> {

    @Fluent
    GenericRefedInterface<T> setValue(T value);

    T getValue();

}