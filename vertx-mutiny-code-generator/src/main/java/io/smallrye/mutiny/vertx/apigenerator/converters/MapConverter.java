package io.smallrye.mutiny.vertx.apigenerator.converters;

import java.util.Map;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

public class MapConverter extends BaseShimTypeConverter {

    public static final String MAP = Map.class.getName();

    @Override
    public boolean accept(ResolvedType type) {
        try {
            return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(MAP);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Type convert(ResolvedType type) {
        ResolvedType keyType = type.asReferenceType().getTypeParametersMap().get(0).b;
        ResolvedType valueType = type.asReferenceType().getTypeParametersMap().get(1).b;
        var convertedValueType = convertType(valueType);
        var convertedKeyType = convertType(keyType);
        return StaticJavaParser.parseClassOrInterfaceType(MAP)
                .setTypeArguments(convertedKeyType, convertedValueType);
    }
}
