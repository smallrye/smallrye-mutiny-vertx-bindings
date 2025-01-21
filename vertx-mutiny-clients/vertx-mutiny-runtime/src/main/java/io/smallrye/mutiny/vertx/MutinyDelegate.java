package io.smallrye.mutiny.vertx;

/**
 * Interface implemented by every generated Mutiny type.
 */
public interface MutinyDelegate {

    /**
     * @return the delegate used by this Mutiny object of generated type
     */
    Object getDelegate();

}
