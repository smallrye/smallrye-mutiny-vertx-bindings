package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.resolution.types.ResolvedType;

public class BoxedPrimitiveTypeConverter extends BaseShimTypeConverter {

    @Override
    public boolean accept(ResolvedType type) {
        try {
            return type.isReferenceType() && type.asReferenceType().isUnboxable();
        } catch (Exception e) {
            return false;
        }
    }
}
