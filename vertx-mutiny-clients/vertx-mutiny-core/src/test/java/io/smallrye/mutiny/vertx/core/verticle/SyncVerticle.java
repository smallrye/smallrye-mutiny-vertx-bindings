package io.smallrye.mutiny.vertx.core.verticle;

import static org.assertj.core.api.Assertions.assertThat;

import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.mutiny.core.Vertx;

public class SyncVerticle extends AbstractVerticle {

    volatile static boolean DEPLOYED = false;

    @Override
    public void start() {
        assertThat(vertx).isNotNull().isInstanceOf(Vertx.class);
        DEPLOYED = true;
    }

    @Override
    public void stop() {
        assertThat(vertx).isNotNull().isInstanceOf(Vertx.class);
        DEPLOYED = false;
    }
}
