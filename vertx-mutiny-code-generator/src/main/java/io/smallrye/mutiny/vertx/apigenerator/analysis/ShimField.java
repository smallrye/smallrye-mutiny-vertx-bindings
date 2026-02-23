package io.smallrye.mutiny.vertx.apigenerator.analysis;

import com.github.javaparser.ast.type.Type;
import com.palantir.javapoet.TypeSpec;

public interface ShimField extends Shim {

    String getName();

    Type getType();

    boolean isStatic();

    boolean isFinal();

    void generate(ShimClass shim, TypeSpec.Builder builder);

}
