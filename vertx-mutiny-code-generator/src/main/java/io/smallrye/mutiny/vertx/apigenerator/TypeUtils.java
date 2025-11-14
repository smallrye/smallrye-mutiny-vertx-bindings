package io.smallrye.mutiny.vertx.apigenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimMethodParameter;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenMethod;

public class TypeUtils {

    public static List<ShimMethodParameter> convertBareToShimParameters(ShimClass shim,
            List<VertxGenMethod.ResolvedParameter> parameters) {
        return convertBareToShimParameters(shim, parameters, false);
    }

    public static boolean isGenericTypeParameter(Type type) {
        var typeNameAsString = type.asClassOrInterfaceType().getNameAsString();
        return isGenericTypeParameter(typeNameAsString);
    }

    public static boolean isGenericTypeParameter(String typeName) {
        // following the convention for naming type parameters
        return typeName.length() == 1 && typeName.toUpperCase().equals(typeName);
    }

    public static List<ShimMethodParameter> convertBareToShimParameters(ShimClass shim,
            List<VertxGenMethod.ResolvedParameter> parameters, boolean readStreamAsPublisher) {
        List<ShimMethodParameter> params = new ArrayList<>();
        for (VertxGenMethod.ResolvedParameter parameter : parameters) {
            Type shimParameterType;
            if (readStreamAsPublisher && TypeUtils.isReadStream(parameter.type())) {
                shimParameterType = StaticJavaParser.parseClassOrInterfaceType("java.util.concurrent.Flow.Publisher") // To avoid $ in the type name
                        .setTypeArguments(shim.getSource().getGenerator().getConverters()
                                .convert(TypeUtils.getFirstParameterizedType(parameter.type())));
            } else {
                shimParameterType = shim.convert(parameter.type());
            }
            params.add(new ShimMethodParameter(parameter.name(), shimParameterType, parameter.type(), parameter.nullable()));
        }
        return params;
    }

    public static List<ShimMethodParameter> convertBareToShimParameters(ShimClass shim, VertxGenMethod method) {
        return convertBareToShimParameters(shim, method.getParameters());
    }

    public static List<ShimMethodParameter> convertBareToShimParameters(ShimClass shim, VertxGenMethod method,
            boolean readStreamAsPublisher) {
        return convertBareToShimParameters(shim, method.getParameters(), readStreamAsPublisher);
    }

    public static List<Type> convertBaseToShimThrows(ShimClass shim, VertxGenMethod method) {
        List<Type> types = new ArrayList<>();
        for (ResolvedType type : method.getThrownExceptions()) {
            types.add(shim.convert(type));
        }
        return types;
    }

    public static Type convertBareToShimReturnType(ShimClass shim, VertxGenMethod method) {
        if (method.isFluent() && !TypeUtils.isFuture(method.getReturnedType())) {
            return shim.getType();
        }
        ResolvedType type = method.getReturnedType();
        return shim.convert(type);
    }

    public static Type buildParameterizedType(Class<?> clazz, Type... parameters) {
        ClassOrInterfaceType type = StaticJavaParser.parseClassOrInterfaceType(clazz.getName());
        if (parameters != null) {
            type.setTypeArguments(parameters);
        }
        return type;
    }

    public static String getFullyQualifiedName(ResolvedType type) {
        if (!type.isReferenceType()) {
            return null;
        }
        return type.asReferenceType().getQualifiedName();
    }

    public static boolean isParameterizedType(ResolvedType type) {
        return (type.isReferenceType() && !type.asReferenceType().typeParametersMap().isEmpty());
    }

    public static List<ResolvedType> getTypeParameters(ResolvedType type) {
        if (!type.isReferenceType()) {
            throw new IllegalArgumentException("The given type is not a reference type: " + type);
        }
        Optional<ResolvedReferenceTypeDeclaration> declaration = type.asReferenceType().getTypeDeclaration();
        if (declaration.isEmpty()) {
            // Hopefully it will not happen - in this case, we have to use the type parameters map, which may not be ordered.
            return type.asReferenceType().typeParametersMap().getTypes();
        } else {
            List<ResolvedType> params = new ArrayList<>();
            for (ResolvedTypeParameterDeclaration parameter : declaration.get().getTypeParameters()) {
                // We cannot use getValue directly, as it may encounter a "container" error followed by a class cast exception.
                // Working around by computing the name directly
                var value = type.asReferenceType().typeParametersMap()
                        .getValueBySignature(type.asReferenceType().getQualifiedName() + "." + parameter.getName())
                        .orElseThrow();
                params.add(value);
            }
            return params;
        }
    }

    public static ResolvedType getFirstParameterizedType(ResolvedType type) {
        if (!type.isReferenceType()) {
            throw new IllegalArgumentException("The given type is not a reference type: " + type);
        }

        List<ResolvedType> parameters = getTypeParameters(type);
        if (parameters == null || parameters.isEmpty()) {
            throw new IllegalArgumentException("The given type is not parameterized: " + type);
        }
        return parameters.get(0);
    }

    public static ResolvedType getSecondParameterizedType(ResolvedType type) {
        if (!type.isReferenceType()) {
            throw new IllegalArgumentException("The given type is not a reference type: " + type);
        }
        List<ResolvedType> parameters = getTypeParameters(type);
        if (parameters == null || parameters.size() < 2) {
            throw new IllegalArgumentException("The given type is not parameterized with at least 2 types: " + type.describe());
        }
        return parameters.get(1);
    }

    public static boolean isFuture(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        return type.asReferenceType().getQualifiedName().equals("io.vertx.core.Future");
    }

    public static boolean isList(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        return type.asReferenceType().getQualifiedName().equals(List.class.getName());
    }

    public static boolean isSet(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        return type.asReferenceType().getQualifiedName().equals(Set.class.getName());
    }

    public static boolean isMap(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        return type.asReferenceType().getQualifiedName().equals(Map.class.getName());
    }

    public static boolean isIterable(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        return type.asReferenceType().getQualifiedName().equals(Iterable.class.getName());
    }

    public static boolean isHandler(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        return type.asReferenceType().getQualifiedName().equals("io.vertx.core.Handler");
    }

    public static boolean isHandlerOfPromise(ResolvedType type) {
        boolean isHandler = type.isReferenceType()
                && type.asReferenceType().getQualifiedName().equals("io.vertx.core.Handler");
        if (isHandler) {
            ResolvedType firstParameterizedType = TypeUtils.getFirstParameterizedType(type);
            if (firstParameterizedType.isWildcard()) {
                firstParameterizedType = firstParameterizedType.erasure();
            }
            if (firstParameterizedType.isReferenceType()) {
                return firstParameterizedType.asReferenceType().getQualifiedName().equals("io.vertx.core.Promise");
            }
        }
        return false;
    }

    public static boolean isConsumer(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        return type.asReferenceType().getQualifiedName().equals(Consumer.class.getName());
    }

    public static boolean isConsumerOfPromise(ResolvedType type) {
        boolean isConsumer = type.isReferenceType()
                && type.asReferenceType().getQualifiedName().equals(Consumer.class.getName());
        if (isConsumer) {
            ResolvedType firstParameterizedType = TypeUtils.getFirstParameterizedType(type);
            if (firstParameterizedType.isWildcard()) {
                firstParameterizedType = firstParameterizedType.erasure();
            }
            return firstParameterizedType.asReferenceType().getQualifiedName().equals("io.vertx.core.Promise");
        }
        return false;
    }

    public static boolean isSupplier(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        return type.asReferenceType().getQualifiedName().equals(Supplier.class.getName());
    }

    public static boolean isFunction(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        return type.asReferenceType().getQualifiedName().equals(Function.class.getName());
    }

    public static boolean isReadStream(ResolvedType type) {
        if (!type.isReferenceType()) {
            return false;
        }
        return type.asReferenceType().getQualifiedName().equals("io.vertx.core.streams.ReadStream");
    }

    public static boolean isSupplierOfFuture(ResolvedType type) {
        boolean isSupplier = type.isReferenceType()
                && type.asReferenceType().getQualifiedName().equals(Supplier.class.getName());
        if (isSupplier) {
            ResolvedType firstParameterizedType = TypeUtils.getFirstParameterizedType(type);
            if (firstParameterizedType.isWildcard()) {
                firstParameterizedType = firstParameterizedType.erasure();
            }
            return firstParameterizedType.asReferenceType().getQualifiedName().equals("io.vertx.core.Future");
        }
        return false;
    }

    public static boolean hasMethodAReadStreamParameter(List<VertxGenMethod.ResolvedParameter> parameters) {
        if (parameters.isEmpty()) {
            return false;
        }
        for (VertxGenMethod.ResolvedParameter parameter : parameters) {
            if (isReadStream(parameter.type())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPublisher(Type shimType) {
        return shimType.isClassOrInterfaceType()
                && shimType.asClassOrInterfaceType().getNameWithScope().equals("java.util.concurrent.Flow.Publisher");
    }
}
