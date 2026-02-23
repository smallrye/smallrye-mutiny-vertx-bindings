package io.smallrye.mutiny.vertx.apigenerator.shims;

import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimCompanionClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;

/**
 * A shim module that adds the companion class to the shim class when it's not concrete.
 * The companion `Impl` is a package private class declared in the same file as the shim class.
 * However, the companion class has a limited set of "shims":
 * <p>
 * - delegate field, accessors and constructor
 * - no-arg constructor
 * - constructor with delegate parameter and type args
 * - if the shim source class implements `ReadStream`, a `toMulti` method
 */
public class CompanionShimModule implements ShimModule {
    @Override
    public boolean accept(ShimClass shim) {
        return !shim.getSource().isConcrete();
    }

    @Override
    public void analyze(ShimClass shim) {
        // We need to declare the companion on the shim:
        shim.addCompanionClass(new ShimCompanionClass(shim.getSource()));
    }
}
