package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import io.smallrye.mutiny.Uni;

import java.util.function.Consumer;

public class ConsumerConverter extends BaseShimTypeConverter {

    public static final String CONSUMER = Consumer.class.getName();
    public static final String PROMISE = "io.vertx.core.Promise";

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
        if (content.asReferenceType().getQualifiedName().equals(PROMISE)) {
            // Consumer<Promise<T>> must be mapped to Uni<T>
            var promiseType = content.asReferenceType().getTypeParametersMap().get(0).b;
            return StaticJavaParser.parseClassOrInterfaceType(Uni.class.getName())
                    .setTypeArguments(convertType(promiseType));
        } else {
            var converted = convertType(content);
            return StaticJavaParser.parseClassOrInterfaceType(Consumer.class.getName())
                    .setTypeArguments(converted);
        }
    }
}
