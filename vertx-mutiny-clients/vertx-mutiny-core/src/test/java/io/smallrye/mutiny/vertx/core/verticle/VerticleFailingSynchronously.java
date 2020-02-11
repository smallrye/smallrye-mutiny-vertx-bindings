package io.smallrye.mutiny.vertx.core.verticle;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

public class VerticleFailingSynchronously extends AbstractVerticle {

    @Override
    public Uni<Void> asyncStart() {
        throw new NullPointerException("boom");
    }
}
