package io.vertx.codegen.extra;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public interface Bar {
    @GenIgnore
    class Impl implements Bar {
    }
}