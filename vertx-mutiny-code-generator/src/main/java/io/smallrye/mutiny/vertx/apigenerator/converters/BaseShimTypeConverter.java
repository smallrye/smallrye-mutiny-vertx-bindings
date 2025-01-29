package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;

public abstract class BaseShimTypeConverter implements ShimTypeConverter {
    protected MutinyGenerator generator;

    @Override
    public ShimTypeConverter configure(MutinyGenerator generator) {
        this.generator = generator;
        return this;
    }

    protected Type convertType(ResolvedType type) {
        return generator.getConverters().convert(type);
    }

    @Override
    public Type convert(ResolvedType type) {
        return StaticJavaParser.parseType(type.describe());
    }
}
