package io.vertx.sources.itf;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;

import java.util.Iterator;
import java.util.function.Function;

@VertxGen
public interface StringInterface extends Handler<String>, Iterable<String>, Iterator<String>, Function<String, String> {
}
