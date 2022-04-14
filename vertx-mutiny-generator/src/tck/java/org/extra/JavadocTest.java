package org.extra;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@VertxGen
public interface JavadocTest {


    /**
     * Do something.
     * @param list list
     * @param handler handler
     */
    void doSomething(List<String> list, Handler<AsyncResult<Void>> handler);

    /**
     * Do something.
     * @param list1 list
     * @param list2  list
     * @param handler handler
     */
    void doSomething2(List<String> list1, List<String> list2, Handler<AsyncResult<Void>> handler);


    /**
     * Do something.
     * @param list list
     */
    <T> void doSomething3(List<T> list, Handler<AsyncResult<Void>> handler);

}
