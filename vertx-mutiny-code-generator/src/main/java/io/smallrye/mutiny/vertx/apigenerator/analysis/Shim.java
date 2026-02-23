package io.smallrye.mutiny.vertx.apigenerator.analysis;

import com.github.javaparser.ast.type.Type;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;

import io.smallrye.mutiny.vertx.apigenerator.types.JavaType;
import io.smallrye.mutiny.vertx.apigenerator.types.TypeDescriber;

public interface Shim {

    ShimModule declaredBy();

    static TypeName getTypeNameFromType(Type type) {
        String val = TypeDescriber.safeDescribeType(type);
        if ("?".equals(val)) {
            return ClassName.OBJECT;
        }
        return JavaType.of(val).toTypeName();
    }
}
