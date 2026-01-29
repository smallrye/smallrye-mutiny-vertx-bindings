package io.smallrye.mutiny.vertx.apigenerator.shims;

import static io.smallrye.mutiny.vertx.apigenerator.JavadocHelper.amendJavadocIfReturnTypeIsNullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.resolution.types.ResolvedType;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import io.smallrye.mutiny.vertx.TypeArg;
import io.smallrye.mutiny.vertx.apigenerator.JavadocHelper;
import io.smallrye.mutiny.vertx.apigenerator.TypeUtils;
import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.Shim;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimMethodParameter;
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
    private static final Logger log = LoggerFactory.getLogger(PlainMethodShimModule.class);

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

            // check that method is not already present in order to keep ONLY the most specialized method
            var identicalNamedMethods = shim.getMethods().stream()
                    .filter(m -> m.getName().equals(method.getName())).toList();
            for (ShimMethod identicalNamedMethod : identicalNamedMethods) {
                // this issue currently only arises with sqlclient.RowSet.iterator() defined twice and returning both an Iterator and a RowIterator
                // TODO find a way to systematically find the most specialized method in order to remove others.
                if (identicalNamedMethod.getReturnType().toString().equals("java.util.Iterator<R>")) {
                    shim.getMethods().remove(identicalNamedMethod);
                }
            }

            if (shim.getSource().getGenerator().getCollectionResult().isVertxGen(TypeUtils.getFullyQualifiedName(returnType))) {
                shim.addMethod(new PlainMethodReturningVertxGen(this, shim, method, false));
                if (TypeUtils.hasMethodAReadStreamParameter(method.getParameters())) {
                    shim.addMethod(new PlainMethodReturningVertxGen(this, shim, method, true));
                }
            } else if ((TypeUtils.isList(returnType) || TypeUtils.isSet(returnType))
                    && shim.isVertxGen(TypeUtils.getFirstParameterizedType(returnType))) {
                shim.addMethod(new PlainMethodReturningCollectionOfVertxGen(this, shim, method, TypeUtils.isSet(returnType)));
            } else if (TypeUtils.isMap(returnType) && shim.isVertxGen(TypeUtils.getSecondParameterizedType(returnType))) {
                shim.addMethod(new PlainMethodReturningMapOfVertxGen(this, shim, method,
                        shim.getVertxGen(TypeUtils.getSecondParameterizedType(returnType))));
            } else if (TypeUtils.isHandler(returnType)) {
                shim.addMethod(new PlainMethodReturningVertxHandler(this, shim, method));
            } else {
                shim.addMethod(new PlainDelegatingMethod(this, shim, method, false));
                if (TypeUtils.hasMethodAReadStreamParameter(method.getParameters())) {
                    shim.addMethod(new PlainDelegatingMethod(this, shim, method, true));
                }
            }
        }
    }

    private static Javadoc adaptJavadoc(ShimClass shim, VertxGenMethod method, List<ShimMethodParameter> parameters) {
        Javadoc javadoc = method.getJavadoc(shim);
        if (method.isReturnTypeNullable()) {
            javadoc = amendJavadocIfReturnTypeIsNullable(javadoc);
        }
        for (ShimMethodParameter parameter : parameters) {
            if (parameter.nullable()) {
                javadoc = JavadocHelper.amendJavadocIfParameterTypeIsNullable(javadoc, parameter);
            }
        }
        return javadoc;
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
            setJavadoc(adaptJavadoc(shim, method, getParameters()));
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
                                NodeList<Type> mutinifiedParameters = type.asClassOrInterfaceType()
                                        .getTypeArguments().orElse(new NodeList<>());
                                int numberOfTypeArgs = parameters.size();
                                StringBuilder p = new StringBuilder();
                                for (int i = 0; i < numberOfTypeArgs; i++) {
                                    // Two cases :
                                    // - (i) the actual type var are available as TypeVarField
                                    //       (it is the case if the class itself is parameterized, they are part of the constructor)
                                    // - (ii) they are not available as TypeVarField, we must build them
                                    if (shim.getFields().stream()
                                            .anyMatch(el -> el instanceof DelegateShimModule.TypeVarField)) {
                                        if (p.isEmpty()) {
                                            p = new StringBuilder("__typeArg_" + i);
                                        } else {
                                            p.append(", __typeArg_").append(i);
                                        }
                                    } else {
                                        var tn2 = JavaType
                                                .of(TypeArg.class.getName() + "<"
                                                        + TypeDescriber.safeDescribeType(mutinifiedParameters.get(i)) + ">")
                                                .toTypeName();
                                        if (mutinifiedParameters.isEmpty())
                                            throw new RuntimeException(String.format("Can't cast the type %s in %s",
                                                    parameters.get(i), shim.getFullyQualifiedName()));
                                        var declaration = String.format("""
                                                        new %s(
                                                        o2 -> %s.newInstance((%s)o2),
                                                        o2 -> o2.getDelegate()
                                                )
                                                """, tn2, mutinifiedParameters.get(i), parameters.get(i)
                                                .asReferenceType().getQualifiedName());
                                        if (p.isEmpty()) {
                                            p = new StringBuilder(declaration);
                                        } else {
                                            p.append(declaration);
                                        }
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
                            if (TypeUtils.isGenericTypeParameter(type) && shim.getFields().stream()
                                    .anyMatch(el -> el instanceof DelegateShimModule.TypeVarField)) {
                                var argTypes = shim.getFields().stream()
                                        .filter(el -> el instanceof DelegateShimModule.TypeVarField)
                                        .filter(el -> el.getType().asClassOrInterfaceType()
                                                .getTypeArguments().get().get(0).toString()
                                                .equals(type.asClassOrInterfaceType().getNameAsString()))
                                        .toList();
                                if (argTypes.isEmpty()) {
                                    code.addStatement("$T __arg_$L = $T.unknown()",
                                            tn,
                                            tp_index,
                                            TypeArg.class);
                                } else {
                                    code.addStatement("$T __arg_$L = $L", tn, tp_index,
                                            argTypes.get(0).getName());
                                }
                            } else {
                                code.addStatement("$T __arg_$L = $T.unknown()",
                                        tn,
                                        tp_index,
                                        TypeArg.class);
                            }
                        }
                        localTypeVars.add("__arg_" + tp_index);
                        tp_index++;
                    }
                    // Create the result using the `newInstance` call with the type args.
                    if (getOriginalMethod().isReturnTypeNullable()) {
                        code.addStatement("return (_res == null) ? null : $T.newInstance(($T)_res, $L)",
                                JavaType.of(shim.getVertxGen(originalReturnType).getShimClassName()).toTypeName(),
                                JavaType.of(originalReturnType.asReferenceType().getQualifiedName()).toTypeName(),
                                String.join(", ", localTypeVars));
                    } else {
                        code.addStatement("return $T.newInstance(($T)_res, $L)",
                                JavaType.of(shim.getVertxGen(originalReturnType).getShimClassName()).toTypeName(),
                                JavaType.of(originalReturnType.asReferenceType().getQualifiedName()).toTypeName(),
                                String.join(", ", localTypeVars));
                    }
                } else {
                    if (originalReturnType.isReferenceType()
                            && !originalReturnType.asReferenceType().typeParametersMap().isEmpty()) {
                        if (getOriginalMethod().isReturnTypeNullable()) {
                            code.addStatement("return (_res == null) ? null : $T.newInstance(($T)_res)",
                                    JavaType.of(shim.getVertxGen(originalReturnType).getShimClassName()).toTypeName(),
                                    JavaType.of(originalReturnType.asReferenceType().getQualifiedName()).toTypeName());
                        } else {
                            code.addStatement("return $T.newInstance(($T)_res)",
                                    JavaType.of(shim.getVertxGen(originalReturnType).getShimClassName()).toTypeName(),
                                    JavaType.of(originalReturnType.asReferenceType().getQualifiedName()).toTypeName());
                        }
                    } else {
                        if (getOriginalMethod().isReturnTypeNullable()) {
                            code.addStatement("return (_res == null) ? null : new $T(_res)",
                                    shim.getVertxGen(originalReturnType).concrete() ? Shim.getTypeNameFromType(getReturnType())
                                            : JavaType.of(shim.getVertxGen(originalReturnType).getShimCompanionName())
                                                    .toTypeName());
                        } else {
                            code.addStatement("return new $T(_res)",
                                    shim.getVertxGen(originalReturnType).concrete() ? Shim.getTypeNameFromType(getReturnType())
                                            : JavaType.of(shim.getVertxGen(originalReturnType).getShimCompanionName())
                                                    .toTypeName());
                        }
                    }
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
            setJavadoc(adaptJavadoc(shim, method, getParameters()));
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
            setJavadoc(adaptJavadoc(shim, method, getParameters()));
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
            setJavadoc(adaptJavadoc(shim, method, getParameters()));
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            // Declaration
            MethodSpec.Builder method = generateDeclaration(shim, builder);

            // Body
            CodeBlock.Builder code = CodeBlock.builder();
            addGeneratedBy(code);

            if (TypeUtils.mustTransformFutureIntoUni(getParameters(), getOriginalMethod().getParameters())) {

                code.addStatement("return getDelegate().$L(io.smallrye.mutiny.vertx.UniHelper.toFuture($L))",
                        getName(),
                        getParameters().get(0).name());
                method.addCode(code.build());
                builder.addMethod(method.build());
                return;
            }
            // For each parameter, we need to convert it to the bare shimType if needed.
            for (var parameter : getParameters()) {
                code.add(parameter.toBareVariableDeclaration("_" + parameter.name(), shim));
            }

            // We just delegate:
            // return getDelegate().method(_param1, _param2);
            // Unless it's void.
            // In the case of static, we need to use the class name instead of `getDelegate()`.
            var originalMethodReturnTypeName = JavaType
                    .of(ResolvedTypeDescriber.describeResolvedType(getOriginalMethod().getReturnedType())).toTypeName();
            // if the method returns a type R and has an available typevar field, we must wrap the result into this type
            if (TypeUtils.isGenericTypeParameter(originalMethodReturnTypeName.toString()) &&
                    shim.getFields().stream().anyMatch(el -> el instanceof DelegateShimModule.TypeVarField)) {
                if (isStatic()) {
                    code.addStatement("return ($T)$L.wrap($T.$L($L))", this.getReturnType().toString(),
                            shim.getFields().stream().filter(el -> el instanceof DelegateShimModule.TypeVarField)
                                    .toList().get(0).getName(),
                            ClassName.bestGuess(shim.getSource().getFullyQualifiedName()),
                            getName(),
                            String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
                } else {
                    if (isFluent()) {
                        code.addStatement("getDelegate().$L($L)", getName(),
                                String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
                        code.addStatement("return this");
                    } else {
                        code.addStatement("return ($L)$L.wrap(getDelegate().$L($L))", (this.getReturnType().toString()),
                                shim.getFields().stream().filter(el -> el instanceof DelegateShimModule.TypeVarField)
                                        .toList().get(0).getName(),
                                getName(),
                                String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
                    }
                }
            } else {
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
                                    String.join(", ", getParameters()
                                            .stream().map(p -> "_" + p.name()).toList()));
                        }
                    }
                }
            }

            method.addCode(code.build());
            builder.addMethod(method.build());
        }
    }

    private static class PlainMethodReturningVertxHandler extends BaseShimMethod {

        private final ResolvedType handlerType;

        public PlainMethodReturningVertxHandler(ShimModule module, ShimClass shim, VertxGenMethod method) {
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
            handlerType = TypeUtils.getFirstParameterizedType(method.getReturnedType());
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

            String args = String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList());
            TypeName handlerTypeName = JavaType.of(handlerType.erasure().describe()).toTypeName();
            var resIsVertxGen = shim.getSource().getGenerator().getCollectionResult()
                    .isVertxGen(handlerTypeName.toString());
            String resName = resIsVertxGen ? "__res.getDelegate()" : "__res";
            if (isStatic()) {
                ClassName target = ClassName.bestGuess(shim.getSource().getFullyQualifiedName());
                code.addStatement("return __res -> $T.$L($L).handle(($T) $L)",
                        target, getName(), args, handlerTypeName, resName);
            } else {
                code.addStatement("return __res -> getDelegate().$L($L).handle(($T) $L)",
                        getName(), args, handlerTypeName, resName);
            }

            method.addCode(code.build());
            builder.addMethod(method.build());
        }
    }
}
