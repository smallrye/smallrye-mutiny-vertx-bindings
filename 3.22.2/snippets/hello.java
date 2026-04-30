///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:smallrye-mutiny-vertx-core:2.6.0

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.mutiny.core.Vertx;

public class hello {

    static class MyVerticle extends AbstractVerticle {

        private long counter = 0L;

        /*
         * Asynchronous start completion notification through a Uni.
         * This is the Mutiny variant of `start(Promise<Void>)` in plain Vert.x.
         */
        @Override
        public Uni<Void> asyncStart() {

            /* 
             * Vert.x stream (ticks every 2 seconds) to Mutiny stream (Multi),
             * then increment a counter.
             */
            vertx.periodicStream(2000L)
                .toMulti()
                .subscribe().with(tick -> counter++);

            /*
             * HTTP endpoint, where `listen` returns a `Uni<HttpServer>`.
             * Notifies of the start procedure completion by replacing and
             * returning the`Uni<HttpServer>` by `Uni<Void>`.
             */
            return vertx.createHttpServer()
                .requestHandler(req -> req.response().endAndForget("@" + counter))
                .listen(8080)
                .onItem()
                    .invoke(() -> System.out.println("See http://127.0.0.1:8080"))
                .onFailure()
                    .invoke(Throwable::printStackTrace)
                .replaceWithVoid();
        }
    }

    /*
     * Main method, deploys a verticle and awaits for the completion with
     * an `*AndAwait()` method.
     */
    public static void main(String... args) {
        var vertx = Vertx.vertx();
        System.out.println("Deployment Starting");
        vertx.deployVerticleAndAwait(MyVerticle::new, new DeploymentOptions());
        System.out.println("Deployment completed");
    }
}
