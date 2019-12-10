package io.smallrye.mutiny.vertx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Associate an Mutiny generated class with its original type,used for mapping the generated classes to their original type.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MutinyGen {

    /**
     * @return the wrapped class
     */
    Class value();

}
