package org.extra;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;

@VertxGen
public interface StaticMethodWithFuture {

    static Future<Void> doSomethingAsync() {
        return Future.succeededFuture();
    }

    static Future<StaticMethodWithFuture> asyncCreate() {
        return Future.succeededFuture(new StaticMethodWithFuture() {
            @Override
            public String sayHello() {
                return "Hello static methods!";
            }
        });
    }

    String sayHello();
}
