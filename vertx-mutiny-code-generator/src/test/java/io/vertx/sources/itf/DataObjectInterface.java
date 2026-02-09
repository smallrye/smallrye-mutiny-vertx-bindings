package io.vertx.sources.itf;

import java.util.Iterator;
import java.util.function.Function;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;

@VertxGen
public interface DataObjectInterface
        extends Handler<MyDataObject>, Iterable<MyDataObject>, Iterator<MyDataObject>, Function<String, MyDataObject> {
}
