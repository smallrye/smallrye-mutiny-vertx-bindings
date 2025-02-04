package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

public class VertxFutureTypeConverter extends BaseShimTypeConverter {

    public static final String FUTURE = "io.vertx.core.Future";
    public static final String UNI = "io.smallrye.mutiny.Uni";

    @Override
    public boolean accept(ResolvedType type) {
        try {
            return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(FUTURE);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Type convert(ResolvedType type) {
        ResolvedType contentType = type.asReferenceType().getTypeParametersMap().get(0).b;
        var converted = convertType(contentType);
        return StaticJavaParser.parseClassOrInterfaceType(UNI)
                .setTypeArguments(converted);
    }
}
