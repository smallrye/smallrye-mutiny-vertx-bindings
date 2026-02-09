package io.vertx.sources.itf;

import java.util.Iterator;
import java.util.function.Function;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;

@VertxGen
public interface VertxGenInterface extends Handler<StringInterface>, Iterable<StringInterface>, Iterator<StringInterface>,
        Function<String, StringInterface> {
}
