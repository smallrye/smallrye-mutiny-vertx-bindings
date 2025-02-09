package io.smallrye.mutiny.vertx.apigenerator.converters;

import java.util.function.Consumer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

public class ConsumerConverter extends BaseShimTypeConverter {

    public static final String CONSUMER = Consumer.class.getName();

    @Override
    public boolean accept(ResolvedType type) {
        try {
            return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(CONSUMER);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Type convert(ResolvedType type) {
        var content = type.asReferenceType().getTypeParametersMap().get(0).b;
        var converted = convertType(content);
        return StaticJavaParser.parseClassOrInterfaceType(Consumer.class.getName())
                .setTypeArguments(converted);
    }
}
