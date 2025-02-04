package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

public class VertxAsyncResultConverter extends BaseShimTypeConverter {

    public static final String ASYNC_RESULT = "io.vertx.core.AsyncResult";

    @Override
    public boolean accept(ResolvedType type) {
        try {
            return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(ASYNC_RESULT);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Type convert(ResolvedType type) {
        var content = type.asReferenceType().getTypeParametersMap().get(0).b;
        var converted = super.convertType(content);
        return StaticJavaParser.parseClassOrInterfaceType("io.vertx.core.AsyncResult")
                .setTypeArguments(converted);
    }
}
