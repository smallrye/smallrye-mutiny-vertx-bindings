package io.vertx.codegen.extra;

import java.util.function.Function;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.codegen.extra.sub.SomeType;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

@VertxGen
public interface UseVertxGenDeclarationsFromAnotherPackage extends
        SomeType,
        Handler<SomeType>,
        ReadStream<SomeType> {
    @CacheReturn
    SomeType foo(SomeType arg);

    void function(Function<SomeType, SomeType> function);

    void asyncResult(Handler<AsyncResult<SomeType>> handler);

    Handler<SomeType> returnHandler();

    Handler<AsyncResult<SomeType>> returnHandlerAsyncResult();

}
