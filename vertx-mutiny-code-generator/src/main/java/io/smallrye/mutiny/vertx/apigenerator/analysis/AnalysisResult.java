package io.smallrye.mutiny.vertx.apigenerator.analysis;

import java.util.List;

public record AnalysisResult(List<ShimClass> shims) {

    public ShimClass getShimFor(String name) {
        for (ShimClass shim : shims) {
            if (shim.getFullyQualifiedName().equalsIgnoreCase(name)) {
                return shim;
            }
        }
        return null;
    }
}
