package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.resolution.types.ResolvedType;

public class VoidConverter extends BaseShimTypeConverter {

    @Override
    public boolean accept(ResolvedType type) {
        try {
            return type.isVoid()
                    || type.isReferenceType() && type.asReferenceType().getQualifiedName().equals("java.lang.Void");
        } catch (Exception e) {
            return false;
        }
    }
}
