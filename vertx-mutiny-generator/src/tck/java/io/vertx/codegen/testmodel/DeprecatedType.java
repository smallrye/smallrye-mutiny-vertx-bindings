package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.VertxGen;

@VertxGen
@Deprecated
public interface DeprecatedType {

    void someMethod();

    @Deprecated
    void someOtherMethod();
}
