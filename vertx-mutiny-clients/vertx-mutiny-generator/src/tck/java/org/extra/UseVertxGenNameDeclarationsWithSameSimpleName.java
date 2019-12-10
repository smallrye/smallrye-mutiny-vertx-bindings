package org.extra;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

import java.util.function.Function;

@VertxGen
public interface UseVertxGenNameDeclarationsWithSameSimpleName extends
        org.extra.sub.UseVertxGenNameDeclarationsWithSameSimpleName,
        Handler<org.extra.sub.UseVertxGenNameDeclarationsWithSameSimpleName>,
        ReadStream<org.extra.sub.UseVertxGenNameDeclarationsWithSameSimpleName> {
    @CacheReturn
    org.extra.sub.UseVertxGenNameDeclarationsWithSameSimpleName foo(
            org.extra.sub.UseVertxGenNameDeclarationsWithSameSimpleName arg);

    void function(
            Function<org.extra.sub.UseVertxGenNameDeclarationsWithSameSimpleName, org.extra.sub.UseVertxGenNameDeclarationsWithSameSimpleName> function);

    void asyncResult(
            Handler<AsyncResult<org.extra.sub.UseVertxGenNameDeclarationsWithSameSimpleName>> handler);

    Handler<org.extra.sub.UseVertxGenNameDeclarationsWithSameSimpleName> returnHandler();

    Handler<AsyncResult<org.extra.sub.UseVertxGenNameDeclarationsWithSameSimpleName>> returnHandlerAsyncResult();
}
