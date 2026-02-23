package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.resolution.types.ResolvedType;

public class StringConverter extends BaseShimTypeConverter {

    @Override
    public boolean accept(ResolvedType type) {
        return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(String.class.getName());
    }

}
