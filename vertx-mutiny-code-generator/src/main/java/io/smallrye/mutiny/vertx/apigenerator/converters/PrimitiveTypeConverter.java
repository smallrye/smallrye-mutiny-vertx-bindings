package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.resolution.types.ResolvedType;

public class PrimitiveTypeConverter extends BaseShimTypeConverter {

    @Override
    public boolean accept(ResolvedType type) {
        return type.isPrimitive();
    }

}
