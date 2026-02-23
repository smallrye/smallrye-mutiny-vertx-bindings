package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.resolution.types.ResolvedType;

public class ThrowableConverter extends BaseShimTypeConverter {

    @Override
    public boolean accept(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        try {
            String name = type.asReferenceType().getQualifiedName();
            if (name.equalsIgnoreCase(Throwable.class.getName())) {
                return true;
            }
            return type.asReferenceType().getAllAncestors()
                    .stream().anyMatch(t -> t.getQualifiedName().equals(Throwable.class.getName()));
        } catch (Exception e) {
            return false;
        }
    }
}
