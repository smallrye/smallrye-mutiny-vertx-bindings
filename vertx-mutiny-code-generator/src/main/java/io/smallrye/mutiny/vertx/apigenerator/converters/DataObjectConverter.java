package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.resolution.types.ResolvedType;

public class DataObjectConverter extends BaseShimTypeConverter {

    @Override
    public boolean accept(ResolvedType type) {
        return type.isReferenceType()
                && generator.getCollectionResult().isDataObject(type.asReferenceType().getQualifiedName());
    }
}
