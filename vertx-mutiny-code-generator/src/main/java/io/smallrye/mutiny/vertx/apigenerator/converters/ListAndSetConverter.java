package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.List;
import java.util.Set;

public class ListAndSetConverter extends BaseShimTypeConverter {

    public static final String LIST = List.class.getName();
    public static final String SET = Set.class.getName();

    @Override
    public boolean accept(ResolvedType type) {
        try {
            return type.isReferenceType() &&
                    (type.asReferenceType().getQualifiedName().equals(LIST)
                            || type.asReferenceType().getQualifiedName().equals(SET));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Type convert(ResolvedType type) {
        var containedType = type.asReferenceType().getTypeParametersMap().get(0).b;
        var converted = convertType(containedType);
        return StaticJavaParser.parseClassOrInterfaceType(type.asReferenceType().erasure().describe())
                .setTypeArguments(converted);
    }
}
