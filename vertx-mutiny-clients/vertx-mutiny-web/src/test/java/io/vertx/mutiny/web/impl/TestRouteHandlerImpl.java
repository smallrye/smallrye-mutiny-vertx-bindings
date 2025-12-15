package io.vertx.mutiny.web.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.OrderListener;
import io.vertx.mutiny.web.TestRouteHandler;

public class TestRouteHandlerImpl implements TestRouteHandler, OrderListener {

    private final AtomicBoolean called = new AtomicBoolean();

    @Override
    public void handle(RoutingContext rc) {
        if (called.get()) {
            rc.response().end();
        } else {
            rc.fail(500);
        }
    }

    @Override
    public void onOrder(int order) {
        called.set(true);
    }

}
