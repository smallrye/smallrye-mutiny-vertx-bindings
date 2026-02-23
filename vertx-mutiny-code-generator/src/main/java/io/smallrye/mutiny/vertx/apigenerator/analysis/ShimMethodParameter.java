package io.smallrye.mutiny.vertx.apigenerator.analysis;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;

import io.smallrye.mutiny.vertx.ReadStreamSubscriber;
import io.smallrye.mutiny.vertx.apigenerator.shims.DelegateShimModule;
import io.smallrye.mutiny.vertx.apigenerator.types.JavaType;
import io.smallrye.mutiny.vertx.apigenerator.types.ResolvedTypeDescriber;
import io.smallrye.mutiny.vertx.apigenerator.utils.TypeUtils;

public record ShimMethodParameter(String name, Type shimType, ResolvedType originalType, boolean nullable) {

    public static final String DELEGATING_CONSUMER_HANDLER = "io.smallrye.mutiny.vertx.DelegatingConsumerHandler";
    public static final String DELEGATING_HANDLER = "io.smallrye.mutiny.vertx.DelegatingHandler";
    public static final String UNI_HELPER = "io.smallrye.mutiny.vertx.UniHelper";

    public static final TypeName DELEGATING_CONSUMER_HANDLER_TYPE_NAME = JavaType.of(DELEGATING_CONSUMER_HANDLER).toTypeName();
    public static final TypeName DELEGATING_HANDLER_TYPE_NAME = JavaType.of(DELEGATING_HANDLER).toTypeName();
    public static final TypeName UNI_HELPER_TYPE_NAME = JavaType.of(UNI_HELPER).toTypeName();

    public CodeBlock toBareVariableDeclaration(String varname, ShimClass shim) {

        if (TypeUtils.isHandlerOfPromise(originalType)) {
            return handleParameterOfTypeHandlerOfPromise(varname);
        }

        if (TypeUtils.isHandler(originalType)) {
            return handleParameterOfTypeHandler(shim, varname);
        }

        if (TypeUtils.isList(originalType)) {
            return handleParameterOfTypeList(shim, varname);
        }

        if (TypeUtils.isSet(originalType)) {
            return handleParameterOfTypeSet(shim, varname);
        }

        if (TypeUtils.isMap(originalType)) {
            return handleParameterOfTypeMap(shim, varname);
        }

        if (TypeUtils.isConsumerOfPromise(originalType)) {
            return handleParameterOfTypeConsumerOfPromise(shim, varname);
        }

        if (TypeUtils.isConsumer(originalType)) {
            return handleParameterOfTypeConsumer(shim, varname);
        }

        if (TypeUtils.isSupplierOfFuture(originalType)) {
            return handleParameterOfTypeSupplierOfFuture(shim, varname);
        }

        if (TypeUtils.isSupplier(originalType)) {
            return handlerParameterOfTypeSupplier(shim, varname);
        }

        if (TypeUtils.isFunction(originalType)) {
            return handleParameterOfTypeFunction(shim, varname);
        }

        if (TypeUtils.isReadStream(originalType) && TypeUtils.isPublisher(shimType)) {
            return handleReadStreamAsPublisher(shim, varname);
        }

        if (shim.isVertxGen(originalType)) {
            if (nullable) {
                return CodeBlock.builder().addStatement("$T $L = $L == null ? null : $L.getDelegate()",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        name).build();
            } else {
                return CodeBlock.builder().addStatement("$T $L = $L.getDelegate()",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name).build();
            }
        } else {
            if (nullable) {
                return CodeBlock.builder().addStatement("$T $L = $L == null ? null : $L",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        name).build();
            } else {
                return CodeBlock.builder().addStatement("$T $L = $L",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name).build();
            }
        }

    }

    private CodeBlock handleParameterOfTypeHandlerOfPromise(String varname) {
        var builder = CodeBlock.builder();
        builder.addStatement("$T $L = io.smallrye.mutiny.vertx.UniHelper.toHandlerOfPromise($L)",
                JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                varname,
                name);
        return builder.build();
    }

    private CodeBlock handleParameterOfTypeFunction(ShimClass shim, String varname) {
        var builder = CodeBlock.builder();
        ResolvedType input = TypeUtils.getFirstParameterizedType(originalType);
        ResolvedType output = TypeUtils.getSecondParameterizedType(originalType);
        boolean inputIsVertxGen = shim.isVertxGen(input);
        boolean outputIsVertxGen = shim.isVertxGen(output);
        boolean isOutputFuture = TypeUtils.isFuture(output);

        // If neither the input nor the output is a Vert.x Gen, we can use the function as is.
        if (!inputIsVertxGen && !outputIsVertxGen && !isOutputFuture) {
            // No nullable case - we can use the function as is, and if null, it will be null.
            return builder.addStatement("$T $L = $L",
                    JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                    varname,
                    name).build();
        }

        if (!inputIsVertxGen && !outputIsVertxGen) {
            // The output is a future - we need to check if it's a Future of Vert.x Gen
            boolean futureOfVertxGen = shim.isVertxGen(TypeUtils.getFirstParameterizedType(output));
            if (futureOfVertxGen) {
                if (nullable) {
                    return builder.addStatement(
                            "$T $L = $L == null ? null : item -> $T.toFuture($L.apply(item).map(i -> i.getDelegate()))",
                            JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                            varname,
                            name,
                            UNI_HELPER_TYPE_NAME,
                            name).build();
                } else {
                    return builder.addStatement("$T $L = item -> $T.toFuture($L.apply(item).map(i -> i.getDelegate()))",
                            JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                            varname,
                            UNI_HELPER_TYPE_NAME,
                            name).build();
                }
            } else {
                if (nullable) {
                    return builder.addStatement("$T $L = $L == null ? null : item -> $T.toFuture($L.apply(item))",
                            JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                            varname,
                            name,
                            UNI_HELPER_TYPE_NAME,
                            name).build();
                } else {
                    return builder.addStatement("$T $L = item -> $T.toFuture($L.apply(item))",
                            JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                            varname,
                            UNI_HELPER_TYPE_NAME,
                            name).build();
                }
            }
        }

        // The input is Vert.x Gen,  but not the output.
        if (inputIsVertxGen && !outputIsVertxGen && !isOutputFuture) {
            TypeName shimClassTypeName = JavaType.of(shim.getVertxGen(input).getShimClassName()).toTypeName();
            if (nullable) {
                return builder.addStatement("$T $L = $L == null ? null : item -> $L.apply(new $T(item))",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        name,
                        shimClassTypeName).build();
            } else {
                return builder.addStatement("$T $L = item -> $L.apply(new $T(item))",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        shimClassTypeName).build();
            }
        }

        if (inputIsVertxGen && !outputIsVertxGen) {
            // The output is a future - we need to check if it's a Future of Vert.x Gen
            boolean futureOfVertxGen = shim.isVertxGen(TypeUtils.getFirstParameterizedType(output));
            if (futureOfVertxGen) {
                TypeName shimClassTypeName = JavaType.of(shim.getVertxGen(input).getShimClassName()).toTypeName();
                if (nullable) {
                    return builder.addStatement(
                            "$T $L = $L == null ? null : item -> $T.toFuture($L.apply(new $T(item)).map(i -> i.getDelegate()))",
                            JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                            varname,
                            name,
                            UNI_HELPER_TYPE_NAME,
                            name,
                            shimClassTypeName).build();
                } else {
                    return builder.addStatement("$T $L = item -> $T.toFuture($L.apply(new $T(item)).map(i -> i.getDelegate()))",
                            JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                            varname,
                            UNI_HELPER_TYPE_NAME,
                            name,
                            shimClassTypeName).build();
                }
            } else {
                TypeName shimClassTypeName = JavaType.of(shim.getVertxGen(input).getShimClassName()).toTypeName();
                if (nullable) {
                    return builder.addStatement("$T $L = $L == null ? null : item -> $T.toFuture($L.apply(new $T(item)))",
                            JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                            varname,
                            name,
                            UNI_HELPER_TYPE_NAME,
                            name,
                            shimClassTypeName).build();
                } else {
                    return builder.addStatement("$T $L = item -> $T.toFuture($L.apply(new $T(item)))",
                            JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                            varname,
                            UNI_HELPER_TYPE_NAME,
                            name,
                            shimClassTypeName).build();
                }
            }
        }

        // The output is Vert.x Gen (we need to unwrap the output), but not the input.
        if (!inputIsVertxGen) {
            if (nullable) {
                return builder.addStatement("$T $L = $L == null ? null : item -> $L.apply(item).getDelegate()",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        name).build();
            } else {
                return builder.addStatement("$T $L = item -> $L.apply(item).getDelegate()",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name).build();
            }
        }

        TypeName shimClassTypeName = JavaType.of(shim.getVertxGen(input).getShimClassName()).toTypeName();
        // Both the input and the output are Vert.x Gen, we need to unwrap the input and wrap the output.
        if (nullable) {
            return builder.addStatement("$T $L = $L == null ? null : item -> $L.apply(new $T(item)).getDelegate()",
                    JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                    varname,
                    name,
                    name,
                    shimClassTypeName)
                    .build();
        } else {
            return builder.addStatement("$T $L = item -> $L.apply(new $T(item)).getDelegate()",
                    JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                    varname,
                    name,
                    shimClassTypeName)
                    .build();
        }

    }

    private CodeBlock handlerParameterOfTypeSupplier(ShimClass shim, String varname) {
        var builder = CodeBlock.builder();
        if (shim.isVertxGen(TypeUtils.getFirstParameterizedType(originalType))) {
            if (nullable) {
                builder.addStatement("$T $L = $L == null ? null : () -> $L.get().getDelegate()",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        name);
            } else {
                builder.addStatement("$T $L = () -> $L.get().getDelegate()",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name);
            }
        } else {
            builder.addStatement("$T $L = $L",
                    JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                    varname,
                    name);
        }
        return builder.build();
    }

    private CodeBlock handleParameterOfTypeSupplierOfFuture(ShimClass shim, String varname) {
        var builder = CodeBlock.builder();
        if (shim.isVertxGen(TypeUtils.getFirstParameterizedType(TypeUtils.getFirstParameterizedType(originalType)))) {
            if (nullable) {
                builder.addStatement("$T $L = $L == null ? null : () -> $T.toFuture($L.get().map(i -> i.getDelegate()))",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        UNI_HELPER_TYPE_NAME,
                        name);
            } else {
                builder.addStatement("$T $L = () -> $T.toFuture($L.get().map(i -> i.getDelegate()))",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        UNI_HELPER_TYPE_NAME,
                        name);
            }
        } else {
            if (nullable) {
                builder.addStatement("$T $L = $L == null ? null : () -> $T.toFuture($L.get())",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        UNI_HELPER_TYPE_NAME,
                        name);
            } else {
                builder.addStatement("$T $L = () -> $T.toFuture($L.get())",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        UNI_HELPER_TYPE_NAME,
                        name);
            }
        }
        return builder.build();
    }

    private CodeBlock handleParameterOfTypeConsumerOfPromise(ShimClass shim, String varname) {
        var builder = CodeBlock.builder();
        builder.addStatement("$T $L = item -> io.smallrye.mutiny.vertx.UniHelper.toPromise($L)",
                JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                varname,
                name);
        return builder.build();
    }

    private CodeBlock handleParameterOfTypeConsumer(ShimClass shim, String varname) {
        var builder = CodeBlock.builder();
        ResolvedType typeOfConsumedItems = TypeUtils.getFirstParameterizedType(originalType);
        if (shim.isVertxGen(typeOfConsumedItems)) {
            // We have a Consumer of Vert.x Gen type, we need to unwrap each item.
            TypeName shimClassTypeName = JavaType.of(shim.getVertxGen(typeOfConsumedItems).getShimClassName()).toTypeName();
            if (nullable) {
                builder.addStatement("$T $L = $L == null ? null : item -> $L.accept(new $T(item))",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        name,
                        shimClassTypeName);
            } else {
                builder.addStatement("$T $L = item -> $L.accept(new $T(item))",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        shimClassTypeName);
            }
        } else {
            // Reuse the consumer as is - no need to check for nullable, it will be `null` in this case.
            builder.addStatement("$T $L = $L",
                    JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                    varname,
                    name);
        }
        return builder.build();
    }

    private CodeBlock handleParameterOfTypeMap(ShimClass shim, String varname) {
        var builder = CodeBlock.builder();
        if (shim.isVertxGen(TypeUtils.getSecondParameterizedType(originalType))) {
            // We have a Map of Vert.x Gen type, we need to unwrap each item.
            if (nullable) {
                builder.addStatement(
                        "$T $L = $L == null ? null : $L.entrySet().stream().collect($T.toMap(java.util.Map.Entry::getKey, entry -> entry.getValue().getDelegate()))",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        name,
                        Collectors.class);
            } else {
                builder.addStatement(
                        "$T $L = $L.entrySet().stream().collect($T.toMap(java.util.Map.Entry::getKey, entry -> entry.getValue().getDelegate()))",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        Collectors.class);
            }
        } else {
            builder.addStatement("$T $L = $L",
                    JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                    varname,
                    name);
        }
        return builder.build();
    }

    private CodeBlock handleParameterOfTypeList(ShimClass shim, String varname) {
        var builder = CodeBlock.builder();
        if (shim.isVertxGen(TypeUtils.getFirstParameterizedType(originalType))) {
            // We have a List of Vert.x Gen type, we need to unwrap each item.
            if (nullable) {
                builder.addStatement(
                        "$T $L = $L == null ? null : $L.stream().map(item -> item.getDelegate()).collect($T.toList())",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        name,
                        Collectors.class);
            } else {
                builder.addStatement("$T $L = $L.stream().map(item -> item.getDelegate()).collect($T.toList())",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        Collectors.class);
            }
        } else {
            builder.addStatement("$T $L = $L",
                    JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                    varname,
                    name);
        }
        return builder.build();
    }

    private CodeBlock handleParameterOfTypeSet(ShimClass shim, String varname) {
        var builder = CodeBlock.builder();
        if (shim.isVertxGen(TypeUtils.getFirstParameterizedType(originalType))) {
            // We have a Set of Vert.x Gen type, we need to unwrap each item.
            if (nullable) {
                builder.addStatement(
                        "$T $L = $L == null ? null : $L.stream().map(item -> item.getDelegate()).collect($T.toSet())",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        name,
                        Collectors.class);
            } else {
                builder.addStatement("$T $L = $L.stream().map(item -> item.getDelegate()).collect($T.toSet())",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        Collectors.class);
            }
        } else {
            builder.addStatement("$T $L = $L",
                    JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                    varname,
                    name);
        }
        return builder.build();
    }

    private CodeBlock handleParameterOfTypeHandler(ShimClass shim, String varname) {
        var builder = CodeBlock.builder();
        // We need a handler, we need to check if the received items are Vert.x gen or not, we also need to check for Void (as we would receive a Runnable and not a Consumer)
        ResolvedType type = TypeUtils.getFirstParameterizedType(originalType);

        if (type.isVoid() || type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(Void.class.getName())) {
            // We have a handler of Void, the parameter is a runnable
            if (nullable) {
                builder.addStatement("$T $L = $L == null ? null : (_ignored -> $L.run())",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        name);
            } else {
                builder.addStatement("$T $L = _ignored -> $L.run()",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name);
            }
        } else if (!shim.isVertxGen(type)) {
            // If the original type is a handler and the type does not require conversion, we need to wrap it into a DelegatingHandler
            if (nullable) {
                builder.addStatement("$T $L = $L == null ? null : new $T<>($L)",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        DELEGATING_CONSUMER_HANDLER_TYPE_NAME,
                        name);
            } else {
                builder.addStatement("$T $L = new $T<>($L)",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        DELEGATING_CONSUMER_HANDLER_TYPE_NAME,
                        name);
            }
        } else {
            // If the original type is a handler and the type requires conversion, we need to wrap it into a DelegatingHandler and a DelegatingConsumerHandler
            TypeName shimClassTypeName = JavaType.of(shim.getVertxGen(type).getShimClassName()).toTypeName();
            if (nullable) {
                builder.addStatement("$T $L = $L == null ? null : new $T<>(new $T<>($L), item -> new $T(item))",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name,
                        DELEGATING_HANDLER_TYPE_NAME,
                        DELEGATING_CONSUMER_HANDLER_TYPE_NAME,
                        name,
                        shimClassTypeName);
            } else {
                if (shim.isVertxGen(type)) {
                    String firstElem = String.format("io.smallrye.mutiny.vertx.MutinyHelper.convertConsumer(%s)",
                            name);

                    var typeParameters = type.asReferenceType().typeParametersValues();
                    String typeParameter = null;
                    var typeParametersToWrite = new ArrayList<String>();
                    for (ResolvedType parameter : typeParameters) {
                        var typeField = shim.getFields().stream()
                                .filter(field -> field instanceof DelegateShimModule.TypeVarField &&
                                        field.getType().asClassOrInterfaceType().getTypeArguments().get().get(0)
                                                .asClassOrInterfaceType().getName().toString().equals(parameter.describe()))
                                .map(ShimField::getName).toList();
                        if (typeField.isEmpty()) {
                            typeParametersToWrite.add("__TYPE_ARG");
                        } else {
                            typeParametersToWrite.add(typeField.get(0));
                        }
                    }
                    builder.addStatement(
                            "$T $L = io.smallrye.mutiny.vertx.MutinyHelper.convertHandler($L, event -> $T.newInstance(($L)event $L))",
                            JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                            varname,
                            firstElem,
                            shimClassTypeName,
                            type.asReferenceType().getQualifiedName(),
                            typeParametersToWrite.isEmpty() ? ""
                                    : String.format(", %s", String.join(",", typeParametersToWrite)));
                } else {
                    builder.addStatement("$T $L = new $T<>(new $T<>($L), item -> new $T(item))",
                            JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                            varname,
                            DELEGATING_HANDLER_TYPE_NAME,
                            DELEGATING_CONSUMER_HANDLER_TYPE_NAME,
                            name,
                            shimClassTypeName);
                }
            }
        }
        return builder.build();
    }

    public CodeBlock handleReadStreamAsPublisher(ShimClass shim, String varname) {
        CodeBlock.Builder builder = CodeBlock.builder();
        ResolvedType originalItemType = TypeUtils.getFirstParameterizedType(originalType);
        if (shim.isVertxGen(originalItemType)) {
            if (nullable) {
                builder.addStatement("$T $L = $L == null ? null : $T.asReadStream($L, obj -> ($T) obj.getDelegate())",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name(),
                        ReadStreamSubscriber.class,
                        name(),
                        JavaType.of((ResolvedTypeDescriber.describeResolvedType(originalItemType))).toTypeName());
            } else {
                builder.addStatement("$T $L = $T.asReadStream($L, obj -> ($T) obj.getDelegate())",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        ReadStreamSubscriber.class,
                        name(),
                        JavaType.of((ResolvedTypeDescriber.describeResolvedType(originalItemType))).toTypeName());
            }
        } else {
            if (nullable) {
                builder.addStatement("$T $L = $L == null ? $T.asReadStream($L, obj -> obj).resume()",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        name(),
                        ReadStreamSubscriber.class,
                        name());
            } else {
                builder.addStatement("$T $L = $T.asReadStream($L, obj -> obj).resume()",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalType)).toTypeName(),
                        varname,
                        ReadStreamSubscriber.class,
                        name());
            }
        }
        return builder.build();
    }
}
