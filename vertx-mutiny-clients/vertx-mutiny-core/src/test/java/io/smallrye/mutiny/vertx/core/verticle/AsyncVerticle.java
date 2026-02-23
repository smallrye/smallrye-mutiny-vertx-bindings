package io.smallrye.mutiny.vertx.core.verticle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.mutiny.core.Vertx;

public class AsyncVerticle extends AbstractVerticle {

    volatile static boolean DEPLOYED = false;

    @Override
    public Uni<Void> asyncStart() {
        assertThat(vertx).isNotNull().isInstanceOf(Vertx.class);
        return vertx.eventBus().consumer("foo")
                .handler(m -> {
                })
                .completion().onItem().invoke(x -> {
                    DEPLOYED = true;
                });
    }

    @Override
    public Uni<Void> asyncStop() {
        assertThat(vertx).isNotNull().isInstanceOf(Vertx.class);
        return Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> null))
                .onItem().transform(x -> {
                    DEPLOYED = false;
                    return null;
                });
    }
}
