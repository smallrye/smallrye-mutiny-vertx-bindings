package io.smallrye.mutiny.vertx.core.verticle;

import java.util.concurrent.CompletableFuture;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

public class VerticleFailingAsynchronously extends AbstractVerticle {

    @Override
    public Uni<Void> asyncStart() {
        return Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> {
            throw new NullPointerException("boom");
        }));
    }
}
