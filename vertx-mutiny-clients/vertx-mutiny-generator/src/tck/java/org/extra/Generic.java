package org.extra;

import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public interface Generic<T> {

    T value();

}
