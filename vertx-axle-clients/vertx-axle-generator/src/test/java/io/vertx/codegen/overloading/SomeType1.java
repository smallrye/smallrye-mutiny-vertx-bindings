package io.vertx.codegen.overloading;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.streams.WriteStream;

@VertxGen
public interface SomeType1 extends WriteStream<String> {

    /**
     * We only redefine {@code end()} and not {@code end(Handler<AsyncResult<Void>>)}
     */
    void end();

}
