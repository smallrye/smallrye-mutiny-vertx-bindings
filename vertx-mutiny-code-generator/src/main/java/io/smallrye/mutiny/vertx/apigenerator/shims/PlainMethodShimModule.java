package io.smallrye.mutiny.vertx.apigenerator.shims;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import io.smallrye.mutiny.vertx.TypeArg;
import io.smallrye.mutiny.vertx.apigenerator.TypeUtils;
import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.Shim;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenClass;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenMethod;
import io.smallrye.mutiny.vertx.apigenerator.types.JavaType;
import io.smallrye.mutiny.vertx.apigenerator.types.ResolvedTypeDescriber;
import io.smallrye.mutiny.vertx.apigenerator.types.TypeDescriber;

/**
 * A shim generating a method delegating to the original method for each method that is not returning a Future.
 * It handles the case where the method returns a {@code Vert.x Gen} object, or a list/map/set of {@code Vert.x Gen} object.
 */
public class PlainMethodShimModule implements ShimModule {
    @Override
    public boolean accept(ShimClass shim) {
        return true;
    }

    @Override
    public void analyze(ShimClass shim) {
        for (VertxGenMethod method : shim.getSource().getMethods()) {
            ResolvedType returnType = method.getReturnedType();
            // Exclude method returning a Future
            if (TypeUtils.isFuture(returnType)) {
                continue; // Handled by the FutureShimModule
            }

            if (shim.getSource().getGenerator().getCollectionResult().isVertxGen(TypeUtils.getFullyQualifiedName(returnType))) {
                shim.addMethod(new PlainMethodReturningVertxGen(this, shim, method, false));
                if (TypeUtils.isMethodAcceptingASingleReadStream(method.getParameters())) {
                    shim.addMethod(new PlainMethodReturningVertxGen(this, shim, method, true));
                }
            } else if ((TypeUtils.isList(returnType) || TypeUtils.isSet(returnType))
                    && shim.isVertxGen(TypeUtils.getFirstParameterizedType(returnType))) {
                shim.addMethod(new PlainMethodReturningCollectionOfVertxGen(this, shim, method, TypeUtils.isSet(returnType)));
            } else if (TypeUtils.isMap(returnType) && shim.isVertxGen(TypeUtils.getSecondParameterizedType(returnType))) {
                shim.addMethod(new PlainMethodReturningMapOfVertxGen(this, shim, method,
                        shim.getVertxGen(TypeUtils.getSecondParameterizedType(returnType))));
            } else {
                shim.addMethod(new PlainDelegatingMethod(this, shim, method, false));
                if (TypeUtils.isMethodAcceptingASingleReadStream(method.getParameters())) {
                    shim.addMethod(new PlainDelegatingMethod(this, shim, method, true));
                }
            }

            //            // TODO: Consumer, Supplier, Iterable, Iterator, Handler, Function, etc.
        }
    }

    private static class PlainMethodReturningVertxGen extends BaseShimMethod {

        private final ResolvedType originalReturnType;

        public PlainMethodReturningVertxGen(ShimModule module, ShimClass shim, VertxGenMethod method,
                boolean readStreamAsPublisher) {
            super(
                    module,
                    method.getName(),
                    TypeUtils.convertBareToShimReturnType(shim, method),
                    TypeUtils.convertBareToShimParameters(shim, method, readStreamAsPublisher),
                    TypeUtils.convertBaseToShimThrows(shim, method),
                    method.isStatic(),
                    method.isFinal(),
                    method.getJavadoc(shim),
                    method);
            originalReturnType = method.getReturnedType();
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            // Declaration
            MethodSpec.Builder method = generateDeclaration(shim, builder);

            // Body
            CodeBlock.Builder code = CodeBlock.builder();
            addGeneratedBy(code);
            // For each parameter, we need to convert it to the bare shimType if needed.
            for (var parameter : getParameters()) {
                code.add(parameter.toBareVariableDeclaration("_" + parameter.name(), shim));
            }

            // Invoke the method
            var originalMethodReturnTypeName = JavaType
                    .of(ResolvedTypeDescriber.describeResolvedType(getOriginalMethod().getReturnedType())).toTypeName();
            if (isStatic()) {
                code.addStatement("$T _res = $T.$L($L)",
                        originalMethodReturnTypeName,
                        JavaType.of(shim.getSource().getFullyQualifiedName()).toTypeName(),
                        getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            } else {
                code.addStatement("$T _res = getDelegate().$L($L)",
                        originalMethodReturnTypeName,
                        getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            }
            if (isFluent()) {
                code.addStatement("return this");
            } else {
                if (!isStatic() && originalReturnType.isReferenceType()
                        && !originalReturnType.asReferenceType().typeParametersMap().isEmpty()) {
                    // Step 1 - Create a TypeArgs instance for every type parameter of the returned type.
                    NodeList<Type> types = getReturnType().asClassOrInterfaceType().getTypeArguments().orElse(new NodeList<>());
                    List<ResolvedType> listOfOriginalTypeParameters = TypeUtils.getTypeParameters(originalReturnType);
                    int tp_index = 0;
                    List<String> localTypeVars = new ArrayList<>();
                    for (Type type : types) {
                        // Check if the given type is Vert.x Gen or not
                        TypeName tn = JavaType.of(TypeArg.class.getName() + "<" + TypeDescriber.safeDescribeType(type) + ">")
                                .toTypeName();
                        if (shim.isVertxGen(listOfOriginalTypeParameters.get(tp_index))) {
                            VertxGenClass gen = shim.getVertxGen(listOfOriginalTypeParameters.get(tp_index));
                            // We need 2 functions:
                            // - wrap (from the bare type to the shim type)
                            // - unwrap (from the shim type to the bare type)

                            // The newInstance method does not have a type arg parameter if the type is not a parameterized type.
                            boolean isParameterized = TypeUtils.isParameterizedType(listOfOriginalTypeParameters.get(tp_index));
                            if (isParameterized) {
                                List<ResolvedType> parameters = TypeUtils
                                        .getTypeParameters(listOfOriginalTypeParameters.get(tp_index));
                                int numberOfTypeArgs = parameters.size();
                                StringBuilder p = new StringBuilder();
                                for (int i = 0; i < numberOfTypeArgs; i++) {
                                    if (p.isEmpty()) {
                                        p = new StringBuilder("__typeArg_" + i);
                                    } else {
                                        p.append(", __typeArg_").append(i);
                                    }
                                }
                                code.addStatement("""
                                        $T __arg_$L = new $T(
                                            o -> $T.newInstance(($T)o, $L),
                                            o -> o.getDelegate()
                                        )
                                        """,
                                        tn, // Type of the local variable
                                        tp_index,
                                        tn, // Constructor type
                                        JavaType.of(gen.getShimClassName()).toTypeName(), // Shim type (erased)
                                        JavaType.of(
                                                listOfOriginalTypeParameters.get(tp_index).asReferenceType().getQualifiedName())
                                                .toTypeName(), // Cast type (bare and erased)
                                        p);
                            } else {
                                code.addStatement("""
                                        $T __arg_$L = new $T(
                                            o -> $T.newInstance(($T)o),
                                            o -> o.getDelegate()
                                        )
                                        """,
                                        tn, // Type of the local variable
                                        tp_index,
                                        tn, // Constructor type
                                        JavaType.of(gen.getShimClassName()).toTypeName(), // Shim type (erased)
                                        JavaType.of(
                                                listOfOriginalTypeParameters.get(tp_index).asReferenceType().getQualifiedName())
                                                .toTypeName() // Cast type (bare and erased)
                                );
                            }

                        } else {
                            code.addStatement("$T __arg_$L = $T.unknown()",
                                    tn,
                                    tp_index,
                                    TypeArg.class);
                        }
                        localTypeVars.add("__arg_" + tp_index);
                        tp_index++;
                    }

                    // Create the result using the `newInstance` call with the type args.
                    code.addStatement("return (_res == null) ? null : $T.newInstance(($T)_res, $L)",
                            JavaType.of(shim.getVertxGen(originalReturnType).getShimClassName()).toTypeName(),
                            JavaType.of(originalReturnType.asReferenceType().getQualifiedName()).toTypeName(),
                            String.join(", ", localTypeVars));
                } else {
                    code.addStatement("return (_res == null) ? null : new $T(_res)",
                            shim.getVertxGen(originalReturnType).concrete() ? Shim.getTypeNameFromType(getReturnType())
                                    : JavaType.of(shim.getVertxGen(originalReturnType).getShimCompanionName()).toTypeName());
                }
            }

            method.addCode(code.build());
            builder.addMethod(method.build());
        }
    }

    private static class PlainMethodReturningCollectionOfVertxGen extends BaseShimMethod {
        private final boolean isSet;
        private final Type shimItemType;
        private final ResolvedType itemType;

        public PlainMethodReturningCollectionOfVertxGen(ShimModule module, ShimClass shim, VertxGenMethod method,
                boolean isSet) {
            super(
                    module,
                    method.getName(),
                    TypeUtils.convertBareToShimReturnType(shim, method),
                    TypeUtils.convertBareToShimParameters(shim, method),
                    TypeUtils.convertBaseToShimThrows(shim, method),
                    method.isStatic(),
                    method.isFinal(),
                    method.getJavadoc(shim),
                    method);
            this.isSet = isSet;
            this.itemType = TypeUtils.getFirstParameterizedType(method.getReturnedType());
            this.shimItemType = shim.convert(TypeUtils.getFirstParameterizedType(method.getReturnedType()));
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            // Declaration
            MethodSpec.Builder method = generateDeclaration(shim, builder);

            // Body
            CodeBlock.Builder code = CodeBlock.builder();
            addGeneratedBy(code);
            // For each parameter, we need to convert it to the bare shimType if needed.
            for (var parameter : getParameters()) {
                code.add(parameter.toBareVariableDeclaration("_" + parameter.name(), shim));
            }

            // List<S> invoke() {
            //     ... params...
            //     List<X> _res = getDelegate().method(_param1, _param2);
            //     return _res.stream().map(S::new).collect(Collectors.toList());
            // }

            // Invoke the method
            var bareResultTypeName = JavaType
                    .of(ResolvedTypeDescriber.describeResolvedType(getOriginalMethod().getReturnedType())).toTypeName();
            if (!isStatic()) {
                code.addStatement("$T _res = getDelegate().$L($L)", bareResultTypeName, getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            } else {

                var bareClassName = JavaType.of(shim.getSource().getFullyQualifiedName()).toTypeName();
                code.addStatement("$T _res = $T.$L($L)",
                        bareResultTypeName, bareClassName,
                        getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            }
            if (isFluent()) {
                code.addStatement("return this");
            } else {
                code.addStatement("return (_res ==null) ? null : _res.stream().map($T::new).collect($T.$L())",
                        shim.getVertxGen(itemType).concrete() ? Shim.getTypeNameFromType(shimItemType)
                                : JavaType.of(shim.getVertxGen(itemType).getShimCompanionName()).toTypeName(),
                        Collectors.class,
                        isSet ? "toSet" : "toList");
            }

            method.addCode(code.build());
            builder.addMethod(method.build());
        }
    }

    private static class PlainMethodReturningMapOfVertxGen extends BaseShimMethod {
        private final Type shimItemType;
        private final ResolvedType itemType;

        public PlainMethodReturningMapOfVertxGen(ShimModule module, ShimClass shim, VertxGenMethod method,
                VertxGenClass vertxGen) {
            super(
                    module,
                    method.getName(),
                    TypeUtils.convertBareToShimReturnType(shim, method),
                    TypeUtils.convertBareToShimParameters(shim, method),
                    TypeUtils.convertBaseToShimThrows(shim, method),
                    method.isStatic(),
                    method.isFinal(),
                    method.getJavadoc(shim),
                    method);
            this.itemType = TypeUtils.getSecondParameterizedType(method.getReturnedType());
            this.shimItemType = shim.convert(TypeUtils.getSecondParameterizedType(method.getReturnedType()));
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            // Declaration
            MethodSpec.Builder method = generateDeclaration(shim, builder);

            // Body
            CodeBlock.Builder code = CodeBlock.builder();
            addGeneratedBy(code);
            // For each parameter, we need to convert it to the bare shimType if needed.
            for (var parameter : getParameters()) {
                code.add(parameter.toBareVariableDeclaration("_" + parameter.name(), shim));
            }

            // Map<T, S> invoke() {
            //     ... params...
            //     Map<T, X> _res = getDelegate().method(_param1, _param2);
            //    return _res.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new S(e.getValue())));
            // }

            var originalMethodReturnTypeName = JavaType
                    .of(ResolvedTypeDescriber.describeResolvedType(getOriginalMethod().getReturnedType())).toTypeName();
            if (isStatic()) {
                code.addStatement("$T _res = $T.$L($L)",
                        originalMethodReturnTypeName,
                        JavaType.of(shim.getSource().getFullyQualifiedName()).toTypeName(),
                        getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
                code.addStatement(
                        "return (_res ==null) ? null : _res.entrySet().stream().collect($T.toMap(Map.Entry::getKey, e -> new $T(e.getValue())))",
                        Collectors.class,
                        shim.getVertxGen(itemType).concrete() ? Shim.getTypeNameFromType(shimItemType)
                                : JavaType.of(shim.getVertxGen(itemType).getShimCompanionName()).toTypeName());
            } else {
                if (isFluent()) {
                    code.addStatement("getDelegate().$L($L)", getName(),
                            String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
                    code.addStatement("return this");
                } else {
                    code.addStatement("$T _res = getDelegate().$L($L)",
                            originalMethodReturnTypeName,
                            getName(),
                            String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
                    code.addStatement(
                            "return (_res ==null) ? null : _res.entrySet().stream().collect($T.toMap(Map.Entry::getKey, e -> new $T(e.getValue())))",
                            Collectors.class,
                            shim.getVertxGen(itemType).concrete() ? Shim.getTypeNameFromType(shimItemType)
                                    : JavaType.of(shim.getVertxGen(itemType).getShimCompanionName()).toTypeName());
                }
            }

            method.addCode(code.build());
            builder.addMethod(method.build());
        }
    }

    private static class PlainDelegatingMethod extends BaseShimMethod {
        private final boolean isVoid;

        public PlainDelegatingMethod(ShimModule module, ShimClass shim, VertxGenMethod method, boolean readStreamAsPublisher) {
            super(
                    module,
                    method.getName(),
                    TypeUtils.convertBareToShimReturnType(shim, method),
                    TypeUtils.convertBareToShimParameters(shim, method, readStreamAsPublisher),
                    TypeUtils.convertBaseToShimThrows(shim, method),
                    method.isStatic(),
                    method.isFinal(),
                    method.getJavadoc(shim),
                    method);
            this.isVoid = method.getReturnedType().isVoid();
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            // Declaration
            MethodSpec.Builder method = generateDeclaration(shim, builder);

            // Body
            CodeBlock.Builder code = CodeBlock.builder();
            addGeneratedBy(code);
            // For each parameter, we need to convert it to the bare shimType if needed.
            for (var parameter : getParameters()) {
                code.add(parameter.toBareVariableDeclaration("_" + parameter.name(), shim));
            }

            // We just delegate:
            // return getDelegate().method(_param1, _param2);
            // Unless it's void.

            // In the case of static, we need to use the class name instead of `getDelegate()`.

            if (isVoid) {
                if (isStatic()) {
                    code.addStatement("$T.$L($L)", ClassName.bestGuess(shim.getSource().getFullyQualifiedName()), getName(),
                            String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
                } else {
                    code.addStatement("getDelegate().$L($L)", getName(),
                            String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
                }
            } else {
                if (isStatic()) {
                    code.addStatement("return $T.$L($L)", ClassName.bestGuess(shim.getSource().getFullyQualifiedName()),
                            getName(),
                            String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
                } else {
                    if (isFluent()) {
                        code.addStatement("getDelegate().$L($L)", getName(),
                                String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
                        code.addStatement("return this");
                    } else {
                        code.addStatement("return getDelegate().$L($L)", getName(),
                                String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
                    }
                }
            }

            method.addCode(code.build());
            builder.addMethod(method.build());
        }
    }
}
