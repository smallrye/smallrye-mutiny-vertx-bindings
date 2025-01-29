package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.resolution.types.ResolvedType;

public class JavaLangObjectConverter extends BaseShimTypeConverter {
    @Override
    public boolean accept(ResolvedType type) {
        return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(Object.class.getName());
    }

}
