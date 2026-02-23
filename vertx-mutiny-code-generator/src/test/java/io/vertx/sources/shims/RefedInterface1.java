package io.vertx.sources.shims;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public interface RefedInterface1 {

    String getString();

    @Fluent
    RefedInterface1 setString(String str);
}