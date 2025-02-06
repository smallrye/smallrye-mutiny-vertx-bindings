package io.smallrye.mutiny.vertx.apigenerator.converters;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.ShimConstants;
import io.smallrye.mutiny.vertx.apigenerator.TypeUtils;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenModule;
import io.smallrye.mutiny.vertx.apigenerator.types.ResolvedTypeDescriber;
import io.smallrye.mutiny.vertx.apigenerator.types.TypeDescriber;

public class VertxGenInterfaceConverter extends BaseShimTypeConverter {

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
        String qualifiedName = type.asReferenceType().getQualifiedName();
        VertxGenModule module = generator.getCollectionResult().getModuleForVertxGen(qualifiedName);
        if (type.asReferenceType().typeParametersMap().isEmpty()) {
            return StaticJavaParser.parseClassOrInterfaceType(ShimConstants
                    .getClassName(module, qualifiedName));
        } else {
            List<String> parameters = new ArrayList<>();
            for (ResolvedType value : TypeUtils.getTypeParameters(type)) {
                if (value.isTypeVariable()) {
                    parameters.add(value.asTypeVariable().describe());
                } else {
                    if (generator.getCollectionResult().isVertxGen(value.asReferenceType().getQualifiedName())) {
                        Type converted = convert(value);
                        parameters.add(TypeDescriber.safeDescribeType(converted));
                    } else {
                        parameters.add(ResolvedTypeDescriber.describeResolvedType(value));
                    }
                }
            }
            return StaticJavaParser.parseClassOrInterfaceType(ShimConstants
                    .getClassName(module, qualifiedName))
                    .setTypeArguments(parameters.stream().map(StaticJavaParser::parseType).toArray(Type[]::new));
        }
    }
}
