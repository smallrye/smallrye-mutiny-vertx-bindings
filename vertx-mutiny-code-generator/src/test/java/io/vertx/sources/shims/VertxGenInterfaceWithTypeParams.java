package io.vertx.sources.shims;

import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public interface VertxGenInterfaceWithTypeParams<X, Y> {

    X hello();

    Y greeting();

}
