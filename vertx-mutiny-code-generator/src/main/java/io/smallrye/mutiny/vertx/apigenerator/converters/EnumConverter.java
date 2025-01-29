package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.resolution.types.ResolvedType;

public class EnumConverter extends BaseShimTypeConverter {

    @Override
    public boolean accept(ResolvedType type) {
        try {
            return type.isReferenceType()
                    && type.asReferenceType().getTypeDeclaration().isPresent()
                    && type.asReferenceType().getTypeDeclaration().get().isEnum();
        } catch (Exception e) {
            return false;
        }
    }

}
