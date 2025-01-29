package io.smallrye.mutiny.vertx.apigenerator.collection;

import io.smallrye.mutiny.vertx.apigenerator.ShimConstants;

/**
 * Represents a Vert.x generated class.
 * This is a preliminary representation of the class, it does not contain any information about the methods or fields.
 * <p>
 * It allows reliably detecting whether a shimType is a Vert.x generated class or not, and compute the corresponding shim class.
 *
 * @param fullyQualifiedName
 * @param module
 * @param concrete
 */
public record VertxGenClass(String fullyQualifiedName, VertxGenModule module, boolean concrete) {

    public String getShimClassName() {
        return ShimConstants.getClassName(module, fullyQualifiedName);
    }

    public String getShimCompanionName() {
        if (concrete) {
            throw new IllegalStateException("Cannot compute the companion name for a concrete class");
        } else {
            return getShimClassName() + "." + getSimpleName() + "Impl";
        }
    }

    private String getSimpleName() {
        return fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
    }
}
