package io.smallrye.mutiny.vertx.apigenerator.analysis;

public interface ShimModule {

    boolean accept(ShimClass shim);

    void analyze(ShimClass shim);

}
