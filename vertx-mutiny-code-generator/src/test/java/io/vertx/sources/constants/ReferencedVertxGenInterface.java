package io.vertx.sources.constants;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public interface ReferencedVertxGenInterface {

    String getString();

    @Fluent
    ReferencedVertxGenInterface setString(String str);
}