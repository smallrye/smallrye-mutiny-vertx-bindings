package io.smallrye.mutiny.vertx.core.verticle;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

public class VerticleFailingSynchronouslyOnStop extends AbstractVerticle {

    @Override
    public Uni<Void> asyncStop() {
        throw new NullPointerException("boom");
    }
}
