package io.smallrye.mutiny.vertx.apigenerator.converters;

import java.util.function.Supplier;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

public class SupplierConverter extends BaseShimTypeConverter {

    public static final String SUPPLIER = Supplier.class.getCanonicalName();

    @Override
    public boolean accept(ResolvedType type) {
        try {
            return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(SUPPLIER);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Type convert(ResolvedType type) {
        ResolvedType suppliedType = type.asReferenceType().getTypeParametersMap().get(0).b;
        return StaticJavaParser
                .parseClassOrInterfaceType(SUPPLIER)
                .setTypeArguments(convertType(suppliedType));
    }
}
