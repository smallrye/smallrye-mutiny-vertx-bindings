package io.smallrye.mutiny.vertx.apigenerator.collection;

/**
 * Thrown when there is nothing to generate: the requested module is not found in the sources, or it does not contain any Vert.x
 * Gen interface (e.g., {@code io.vertx:vertx-bridge-common} in Vert.x 5).
 */
public class NothingToGenerateException extends RuntimeException {

    public NothingToGenerateException(String message) {
        super(message);
    }
}
