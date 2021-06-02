package io.vertx.mutiny.junit5;

import static io.vertx.junit5.VertxExtension.DEFAULT_TIMEOUT_DURATION;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import io.vertx.core.VertxException;
import io.vertx.junit5.ParameterClosingConsumer;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxExtensionParameterProvider;
import io.vertx.mutiny.core.Vertx;

public class VertxParameterProvider implements VertxExtensionParameterProvider<Vertx> {

    @Override
    public Class<Vertx> type() {
        return Vertx.class;
    }

    @Override
    public String key() {
        return VertxExtension.VERTX_INSTANCE_KEY;
    }

    @Override
    public Vertx newInstance(ExtensionContext extensionContext, ParameterContext parameterContext) {
        return Vertx.vertx();
    }

    @Override
    public ParameterClosingConsumer<Vertx> parameterClosingConsumer() {
        return vertx -> {
            try {
                vertx.close().await().atMost(Duration.of(DEFAULT_TIMEOUT_DURATION, ChronoUnit.SECONDS));
            } catch (Throwable err) {
                if (err instanceof TimeoutException) {
                    throw err;
                } else if (err instanceof CompletionException) {
                    throw new VertxException(err.getCause());
                } else if (err instanceof Exception) {
                    throw err;
                } else {
                    throw new VertxException(err);
                }
            }
        };
    }
}
