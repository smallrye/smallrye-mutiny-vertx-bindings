package io.vertx.sources.itf;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;

import java.util.Iterator;
import java.util.function.Function;

@VertxGen
public interface VertxGenInterface extends Handler<StringInterface>, Iterable<StringInterface>, Iterator<StringInterface>,
        Function<String, StringInterface> {
}
