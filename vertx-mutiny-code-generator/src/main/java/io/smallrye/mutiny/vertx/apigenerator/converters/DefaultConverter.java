package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.types.ResolvedTypeDescriber;
import io.smallrye.mutiny.vertx.apigenerator.utils.TypeUtils;

public class DefaultConverter extends BaseShimTypeConverter {

    private MutinyGenerator generator;

    @Override
    public ShimTypeConverter configure(MutinyGenerator generator) {
        this.generator = generator;
        return this;
    }

    @Override
    public boolean accept(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        String name = type.asReferenceType().getQualifiedName();
        return generator.getCollectionResult().isVertxGen(name);
    }

    @Override
    public Type convert(ResolvedType type) {
        if (!type.isReferenceType() || type.asReferenceType().typeParametersMap().isEmpty()) {
            String described = ResolvedTypeDescriber.describeResolvedType(type);
            return StaticJavaParser.parseType(described);
        } else {
            NodeList<Type> parameters = new NodeList<>();
            for (ResolvedType value : TypeUtils.getTypeParameters(type)) {
                parameters.add(generator.getConverters().convert(value));
            }
            String described = ResolvedTypeDescriber.describeResolvedType(type);
            return StaticJavaParser.parseClassOrInterfaceType(described)
                    .setTypeArguments(parameters);
        }
    }
}
