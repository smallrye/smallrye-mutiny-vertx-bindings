package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.utils.Pair;

import java.util.List;
import java.util.function.Function;

public class FunctionConverter extends BaseShimTypeConverter {

    public static final String FUNCTION = Function.class.getName();

    @Override
    public boolean accept(ResolvedType type) {
        try {
            return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(FUNCTION);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Type convert(ResolvedType type) {
        List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> map = type.asReferenceType().getTypeParametersMap();
        var inputType = map.get(0).b;
        var outputType = map.get(1).b;
        var convertedInputType = convertType(inputType);
        var convertedOutputType = convertType(outputType);

        return StaticJavaParser
                .parseClassOrInterfaceType(FUNCTION)
                .setTypeArguments(convertedInputType, convertedOutputType);
    }
}
