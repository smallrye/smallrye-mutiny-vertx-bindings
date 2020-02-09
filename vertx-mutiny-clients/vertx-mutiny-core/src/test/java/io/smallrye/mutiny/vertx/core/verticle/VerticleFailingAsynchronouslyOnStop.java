package io.smallrye.mutiny.vertx.core.verticle;

import java.util.concurrent.CompletableFuture;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

public class VerticleFailingAsynchronouslyOnStop extends AbstractVerticle {

    @Override
    public Uni<Void> asyncStop() {
        return Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> {
            throw new NullPointerException("boom");
        }));
    }
}
