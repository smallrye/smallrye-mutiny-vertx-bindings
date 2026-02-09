package io.smallrye.mutiny.vertx.apigenerator.analysis;

import java.util.List;

import com.github.javaparser.ast.type.Type;
import com.palantir.javapoet.TypeSpec;

public interface ShimConstructor extends Shim {

    List<ShimMethodParameter> getParameters();

    List<Type> getThrows();

    void generate(ShimClass shim, TypeSpec.Builder builder);

}
