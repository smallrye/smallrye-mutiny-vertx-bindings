package io.smallrye.mutiny.vertx.apigenerator.converters;

import java.util.function.Consumer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

public class VertxHandlerConverter extends BaseShimTypeConverter {

    public static final String HANDLER = "io.vertx.core.Handler";

    @Override
    public boolean accept(ResolvedType type) {
        try {
            return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(HANDLER);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Type convert(ResolvedType type) {
        var content = type.asReferenceType().getTypeParametersMap().getFirst().b;
        var converted = convertType(content);

        // If converted is Void -> Runnable
        // Otherwise -> Consumer

        if (converted.isVoidType()
                || converted.isReferenceType() && converted.asReferenceType().asString().equals("java.lang.Void")) {
            return StaticJavaParser.parseClassOrInterfaceType(Runnable.class.getName());
        }

        return StaticJavaParser.parseClassOrInterfaceType(Consumer.class.getName())
                .setTypeArguments(converted);
    }
}
