package io.smallrye.mutiny.vertx.apigenerator.analysis;

import com.github.javaparser.ast.type.Type;
import com.palantir.javapoet.TypeSpec;

import java.util.List;

public interface ShimConstructor extends Shim {

    List<ShimMethodParameter> getParameters();

    List<Type> getThrows();

    void generate(ShimClass shim, TypeSpec.Builder builder);

}
