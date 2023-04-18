package io.smallrye.mutiny.vertx.core;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;

public class VertxContextExecutor implements Executor {

    private final Supplier<Context> contextSupplier;

    private VertxContextExecutor(Supplier<Context> contextSupplier) {
        this.contextSupplier = contextSupplier;
    }

    public static VertxContextExecutor fromCaller() {
        Context context = Vertx.currentContext();
        if (context == null) {
            throw new IllegalStateException("Not called from a Vert.x context");
        }
        return of(context);
    }

    public static VertxContextExecutor of(Context context) {
        nonNull(context, "context");
        return new VertxContextExecutor(() -> context);
    }

    public static VertxContextExecutor of(Vertx vertx) {
        nonNull(vertx, "vertx");
        return new VertxContextExecutor(vertx::getOrCreateContext);
    }

    @Override
    public void execute(Runnable command) {
        contextSupplier.get().runOnContext(command);
    }
}
