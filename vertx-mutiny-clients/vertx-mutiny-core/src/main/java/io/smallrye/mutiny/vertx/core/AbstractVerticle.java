package io.smallrye.mutiny.vertx.core;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class AbstractVerticle extends io.vertx.core.AbstractVerticle {

    protected io.vertx.mutiny.core.Vertx vertx;

    /**
     * Initialise the verticle.
     * This is called by Vert.x when the verticle instance is deployed. Don't call it yourself.
     *
     * @param vertx the deploying Vert.x instance
     * @param context the context of the verticle
     */
    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        this.vertx = new io.vertx.mutiny.core.Vertx(vertx);
    }

    /**
     * Start the verticle.
     * This is called by Vert.x when the verticle instance is deployed. Don't call it yourself.
     * <p>
     * If your verticle does things in its startup which take some time then you can override this method
     * and call the {@code startPromise} some time later when start up is complete.
     *
     * <strong>NOTE:</strong> Using {@link #asyncStart()} is recommended.
     *
     * @param startPromise a promise which should be called when verticle start-up is complete.
     * @throws Exception
     */
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        Uni<Void> uni = asyncStart();
        if (uni != null) {
            uni.subscribe().with(x -> {
                startPromise.complete(null);
            }, startPromise::fail);
        } else {
            super.start(startPromise);
        }
    }

    /**
     * Stop the verticle.
     * <p>
     * This is called by Vert.x when the verticle instance is un-deployed. Don't call it yourself.
     * If your verticle does things in its shut-down which take some time then you can override this method
     * and call the {@code stopPromise} some time later when clean-up is complete.
     *
     * <strong>NOTE:</strong> Using {@link #asyncStop()} is recommended.
     *
     * @param stopPromise a promise which should be called when verticle clean-up is complete.
     * @throws Exception
     */
    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        Uni<Void> uni = asyncStop();
        if (uni != null) {
            uni.subscribe().with(x -> stopPromise.complete(), stopPromise::fail);
        } else {
            super.stop(stopPromise);
        }
    }

    /**
     * Start the verticle.
     * This is called by Vert.x when the verticle instance is deployed. Don't call it yourself.
     * <p>
     * If your verticle does things in its startup which take some time then you can override this method
     * and returns a {@link Uni} completed with the start up is complete. Propagating a failure fails the deployment
     * of the verticle
     *
     * @return a {@link Uni} completed when the start up completes, or failed if the verticle cannot be started.
     */
    public Uni<Void> asyncStart() {
        return null;
    }

    /**
     * Stop the verticle.
     * <p>
     * This is called by Vert.x when the verticle instance is un-deployed. Don't call it yourself.
     *
     * If your verticle does things in its shut-down which take some time then you can override this method
     * and returns an {@link Uni} completed when the clean-up is complete.
     *
     * @return a {@link Uni} completed when the clean-up completes, or failed if the verticle cannot be stopped gracefully.
     */
    public Uni<Void> asyncStop() {
        return null;
    }
}