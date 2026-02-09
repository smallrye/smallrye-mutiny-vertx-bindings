package io.smallrye.mutiny.vertx.apigenerator.analysis;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenMethod;

import java.util.List;

public interface ShimMethod extends Shim {

    String getName();

    Type getReturnType();

    List<ShimMethodParameter> getParameters();

    List<Type> getThrows();

    boolean isStatic();

    boolean isFinal();

    boolean isFluent();

    Javadoc getJavadoc();

    VertxGenMethod getOriginalMethod();

    boolean isOverridden();

    void generate(ShimClass shim, TypeSpec.Builder builder);

    MethodSpec.Builder generateDeclaration(ShimClass shim, TypeSpec.Builder builder);

}
