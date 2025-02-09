package io.smallrye.mutiny.vertx.apigenerator.shims;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.resolution.types.ResolvedType;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.apigenerator.JavadocHelper;
import io.smallrye.mutiny.vertx.apigenerator.TypeUtils;
import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.Shim;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimMethodParameter;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenMethod;
import io.smallrye.mutiny.vertx.apigenerator.types.JavaType;
import io.smallrye.mutiny.vertx.apigenerator.types.ResolvedTypeDescriber;
import io.smallrye.mutiny.vertx.apigenerator.types.TypeDescriber;

/**
 * A shim generating a method delegating to the original method for each method that is returning a Future.
 * It handles the case where the method returns a Future of {@code Vert.x Gen} object, or a list/map/set of {@code Vert.x Gen}
 * object.
 */
public class UniMethodShimModule implements ShimModule {

    @Override
    public boolean accept(ShimClass shim) {
        return true;
    }

    @Override
    public void analyze(ShimClass shim) {
        for (VertxGenMethod method : shim.getSource().getMethods()) {
            ResolvedType returnType = method.getReturnedType();
            // Exclude method returning a Future
            if (!TypeUtils.isFuture(returnType)) {
                continue;
            }

            ResolvedType futureItemType = TypeUtils.getFirstParameterizedType(returnType);

            if (shim.getSource().getGenerator().getCollectionResult()
                    .isVertxGen(TypeUtils.getFullyQualifiedName(futureItemType))) {
                shim.addMethod(new UniMethodReturningVertxGen(this, shim, method, false));
                if (TypeUtils.hasMethodAReadStreamParameter(method.getParameters())) {
                    shim.addMethod(new UniMethodReturningVertxGen(this, shim, method, true));
                }
            } else if ((TypeUtils.isList(futureItemType) || TypeUtils.isSet(futureItemType))
                    && shim.isVertxGen(TypeUtils.getFirstParameterizedType(futureItemType))) {
                shim.addMethod(new UniMethodReturningCollectionOfVertxGen(this, shim, method, TypeUtils.isSet(futureItemType)));
            } else if (TypeUtils.isMap(futureItemType)
                    && shim.isVertxGen(TypeUtils.getSecondParameterizedType(futureItemType))) {
                shim.addMethod(new UniMethodReturningMapOfVertxGen(this, shim, method));
            } else {
                shim.addMethod(new UniDelegatingMethod(this, shim, method, false));
                if (TypeUtils.hasMethodAReadStreamParameter(method.getParameters())) {
                    shim.addMethod(new UniDelegatingMethod(this, shim, method, true));
                }
            }
        }
    }

    private static Javadoc adaptParameterJavadoc(ShimClass shim, Javadoc javadoc, List<ShimMethodParameter> parameters) {
        if (javadoc == null) {
            return null;
        }
        for (ShimMethodParameter parameter : parameters) {
            if (parameter.nullable()) {
                javadoc = JavadocHelper.amendJavadocIfParameterTypeIsNullable(javadoc, parameter);
            }
        }
        return javadoc;
    }

    private static Javadoc adaptJavadocToUni(ShimClass shim, VertxGenMethod method) {
        var jd = JavadocHelper.addToJavadoc(method.getJavadoc(shim), """
                <p>
                Unlike the <em>bare</em> Vert.x variant, this method returns a {@link %s Uni}.
                The uni emits the result of the operation as item.
                If the operation fails, the uni emits the failure.
                </p>
                <p>
                Don't forget to <em>subscribe</em> on it to trigger the operation.
                </p>
                """.formatted(Uni.class.getName()));
        jd = JavadocHelper.replace(jd, " future ", " uni ");

        String signature = method.getMethod().getSignature().asString();
        return JavadocHelper
                .addOrReplaceReturnTag(jd,
                        "A {@link %s Uni} representing the asynchronous result of this operation."
                                .formatted(Uni.class.getName()))
                .addBlockTag("see", "%s#%s"
                        .formatted(shim.getSource().getFullyQualifiedName(), signature));
    }

    private static Javadoc adaptJavadocToAndForget(ShimClass shim, Type shimElementType, VertxGenMethod method) {
        var jd = JavadocHelper.addToJavadoc(method.getJavadoc(shim), """
                <p>
                Unlike the <em>bare</em> Vert.x variant, this method ignores the {@link %s} result or any failure.
                </p>
                """.formatted(shimElementType.asString()));
        jd = JavadocHelper.replace(jd, " future ", " underlying uni ");

        String signature = method.getMethod().getSignature().asString();
        return JavadocHelper
                .addOrReplaceReturnTag(jd, "The current instance to chain operations if needed.")
                .addBlockTag("see", "%s#%s"
                        .formatted(shim.getSource().getFullyQualifiedName(), signature));
    }

    private static Javadoc adaptJavadocToAndAwait(ShimClass shim, Type shimElementType, VertxGenMethod method) {
        var jd = JavadocHelper.addToJavadoc(method.getJavadoc(shim),
                """
                        <p>
                        Unlike the <em>bare</em> Vert.x variant, this method returns a {@link %s}.
                        This method awaits <strong>indefinitely</strong> for the completion of the underlying asynchronous operation.
                        If the operation completes successfully, the result is returned, otherwise the failure is thrown (potentially wrapped in a {@code RuntimeException}).
                        </p>
                        """
                        .formatted(shimElementType.asString()));
        jd = JavadocHelper.replace(jd, " future ", " underlying uni ");

        String signature = method.getMethod().getSignature().asString();

        if (shimElementType.isVoidType() || shimElementType.asString().equals(Void.class.getName())) {
            return JavadocHelper
                    .removeReturnTag(jd)
                    .addBlockTag("see", "%s#%s"
                            .formatted(shim.getSource().getFullyQualifiedName(), signature));
        }

        return JavadocHelper
                .addOrReplaceReturnTag(jd, "The operation result")
                .addBlockTag("see", "%s#%s"
                        .formatted(shim.getSource().getFullyQualifiedName(), signature));
    }

    /**
     * The original method is returning a {@code Future<Vert.x Gen>} object.
     * We just delegate to the original method and wrap the result into a {@code Uni}:
     * <code>
     * <pre>
     * Uni&lt;S&gt; method(...) {
     *     ... params...
     *     return Uni.createFrom().completionStage(() -> getDelegate().method(_param1, _param2).toCompletionStage()).map(S::new);
     * }
     * </pre>
     * </code>
     */
    private static class UniMethodReturningVertxGen extends BaseShimMethod {
        private final ResolvedType itemType;
        private final Type shimItemType;

        public UniMethodReturningVertxGen(ShimModule module, ShimClass shim, VertxGenMethod method,
                boolean publisherSignature) {
            super(
                    module,
                    method.getName(),
                    TypeUtils.convertBareToShimReturnType(shim, method),
                    TypeUtils.convertBareToShimParameters(shim, method, publisherSignature),
                    TypeUtils.convertBaseToShimThrows(shim, method),
                    method.isStatic(),
                    method.isFinal(),
                    adaptJavadocToUni(shim, method),
                    method);
            itemType = TypeUtils.getFirstParameterizedType(getOriginalMethod().getReturnedType());
            shimItemType = shim.convert(itemType);
            setJavadoc(adaptParameterJavadoc(shim, getJavadoc(), getParameters()));
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            var futureTypeName = JavaType.of(ResolvedTypeDescriber.describeResolvedType(getOriginalMethod().getReturnedType()))
                    .toTypeName();
            var shimTypeName = JavaType.of(TypeDescriber.safeDescribeType(shimItemType)).toTypeName();

            // Declaration
            MethodSpec.Builder method = generateDeclaration(shim, builder);

            // Body
            CodeBlock.Builder code = CodeBlock.builder();
            addGeneratedBy(code);
            // For each parameter, we need to convert it to the bare shimType if needed.
            for (var parameter : getParameters()) {
                code.add(parameter.toBareVariableDeclaration("_" + parameter.name(), shim));
            }

            // Delegate, Create Uni and transform the item:
            // Supplier<Future<X>> _res = () -> getDelegate().method(_param1, _param2);
            // return Uni.createFrom().completionStage(() -> _res.get().toCompletionStage()).map(S::new);

            if (isStatic()) {
                code.addStatement("$T<$T> _res = () -> $T.$L($L)", ClassName.get(Supplier.class), futureTypeName,
                        JavaType.of(shim.getSource().getFullyQualifiedName()).toTypeName(),
                        getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            } else {
                code.addStatement("$T<$T> _res = () -> getDelegate().$L($L)", ClassName.get(Supplier.class), futureTypeName,
                        getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            }

            code.addStatement("return $T.createFrom().completionStage(() -> _res.get().toCompletionStage()).map($T::new)",
                    Uni.class,
                    shim.getVertxGen(itemType).concrete() ? shimTypeName
                            : JavaType.of(shim.getVertxGen(itemType).getShimCompanionName()).toTypeName());

            method.addCode(code.build());
            builder.addMethod(method.build());

            // Extra methods
            builder.addMethod(generateAwaitMethod(shim, this, shimItemType, getOriginalMethod().getJavadoc(shim)).build());
            builder.addMethod(generateForgetMethod(shim, this, shimItemType, getOriginalMethod().getJavadoc(shim)).build());
        }
    }

    /**
     * The original method is returning a {@code Future<List<X>>} or {@code Future<Set<X>>} where {@code X} is a
     * {@code Vert.x Gen} object.
     * We just delegate to the original method and wrap the result into a {@code Uni}, and for each item in the list, we create
     * a new instance of the shim type:
     * <code>
     * <pre>
     * Uni&lt;List&lt;S&gt;&gt; method(...) {
     *     ... params...
     *     Future&lt;List&lt;X&gt;&gt; _res = getDelegate().method(_param1, _param2);
     *     return Uni.createFrom().completionStage(() -> _res.toCompletionStage()).map(list -> list.stream().map(S::new).collect(Collectors.toList()));
     * }
     * </pre>
     * </code>
     */
    private static class UniMethodReturningCollectionOfVertxGen extends BaseShimMethod {
        private final boolean isSet;
        private final Type shimElementType;
        private final ResolvedType originalElementType;

        public UniMethodReturningCollectionOfVertxGen(ShimModule module, ShimClass shim, VertxGenMethod method, boolean isSet) {
            super(
                    module,
                    method.getName(),
                    TypeUtils.convertBareToShimReturnType(shim, method),
                    TypeUtils.convertBareToShimParameters(shim, method),
                    TypeUtils.convertBaseToShimThrows(shim, method),
                    method.isStatic(),
                    method.isFinal(),
                    adaptJavadocToUni(shim, method),
                    method);
            this.isSet = isSet;
            this.shimElementType = shim.convert(TypeUtils
                    .getFirstParameterizedType(TypeUtils.getFirstParameterizedType(getOriginalMethod().getReturnedType())));
            this.originalElementType = TypeUtils
                    .getFirstParameterizedType(TypeUtils.getFirstParameterizedType(getOriginalMethod().getReturnedType()));
            setJavadoc(adaptParameterJavadoc(shim, getJavadoc(), getParameters()));
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            var futureTypeName = JavaType.of(getOriginalMethod().getReturnedType().describe()).toTypeName();
            var elementTypeName = JavaType.of(TypeDescriber.safeDescribeType(shimElementType)).toTypeName();
            // Declaration
            MethodSpec.Builder method = generateDeclaration(shim, builder);

            // Body
            CodeBlock.Builder code = CodeBlock.builder();
            addGeneratedBy(code);
            // For each parameter, we need to convert it to the bare shimType if needed.
            for (var parameter : getParameters()) {
                code.add(parameter.toBareVariableDeclaration("_" + parameter.name(), shim));
            }

            // Uni<List<S>> method(...) { // Or Set instead of List
            //     ... params...
            //     Supplier<Future<List<X>>> _res = () -> getDelegate().method(_param1, _param2); // Or Set instead of List
            //     return Uni.createFrom().completionStage(() -> _res.get().toCompletionStage()).map(list -> list.stream().map(S::new).collect(Collectors.toList())); // Or toSet()
            // }

            // Invoke the method
            if (isStatic()) {
                code.addStatement("$T<$T> _res = () -> $T.$L($L)", Supplier.class, futureTypeName,
                        JavaType.of(shim.getSource().getFullyQualifiedName()).toTypeName(),
                        getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            } else {
                code.addStatement("$T<$T> _res = () -> getDelegate().$L($L)", Supplier.class, futureTypeName, getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            }
            code.addStatement(
                    "return $T.createFrom().completionStage(() -> _res.get().toCompletionStage()).map(__list -> __list.stream().map($T::new).collect($T.$L()))",
                    Uni.class,
                    shim.getVertxGen(originalElementType).concrete() ? elementTypeName
                            : JavaType.of(shim.getVertxGen(originalElementType).getShimCompanionName()).toTypeName(),
                    Collectors.class,
                    isSet ? "toSet" : "toList");

            method.addCode(code.build());
            builder.addMethod(method.build());

            // Extra methods
            Type collectionType = shim.convert(TypeUtils.getFirstParameterizedType(getOriginalMethod().getReturnedType()));
            builder.addMethod(generateAwaitMethod(shim, this, collectionType, getOriginalMethod().getJavadoc(shim)).build());
            builder.addMethod(generateForgetMethod(shim, this, collectionType, getOriginalMethod().getJavadoc(shim)).build());
        }
    }

    /**
     * The original method is returning a {@code Future<Map<K, V>>} where {@code V} is a {@code Vert.x Gen} object.
     * We just delegate to the original method and wrap the result into a {@code Uni}, and for each value in the map, we create
     * a new instance of the shim type:
     * <code>
     * <pre>
     * Uni&lt;Map&lt;K, S&gt;&gt; method(...) {
     *     ... params...
     *     Future&lt;Map&lt;K, V&gt;&gt; _res = getDelegate().method(_param1, _param2);
     *     return Uni.createFrom().completionStage(() -> _res.toCompletionStage()).map(map -> map.entrySet().stream()
     *      .collect(Collectors.toMap(Map.Entry::getKey, e -> new S(e.getValue())));
     * }
     * </pre>
     * </code>
     */
    private static class UniMethodReturningMapOfVertxGen extends BaseShimMethod {

        private final Type shimValueType;
        private final ResolvedType originalValueType;

        public UniMethodReturningMapOfVertxGen(ShimModule module, ShimClass shim, VertxGenMethod method) {
            super(
                    module,
                    method.getName(),
                    TypeUtils.convertBareToShimReturnType(shim, method),
                    TypeUtils.convertBareToShimParameters(shim, method),
                    TypeUtils.convertBaseToShimThrows(shim, method),
                    method.isStatic(),
                    method.isFinal(),
                    adaptJavadocToUni(shim, method),
                    method);
            var future = getOriginalMethod().getReturnedType();
            var map = TypeUtils.getFirstParameterizedType(future);
            originalValueType = TypeUtils.getSecondParameterizedType(map);
            this.shimValueType = shim.convert(originalValueType);
            setJavadoc(adaptParameterJavadoc(shim, getJavadoc(), getParameters()));
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            var futureTypeName = JavaType.of(getOriginalMethod().getReturnedType().describe()).toTypeName();
            var shimValueTypeName = JavaType.of(TypeDescriber.safeDescribeType(shimValueType)).toTypeName();

            // Declaration
            MethodSpec.Builder method = generateDeclaration(shim, builder);

            // Body
            CodeBlock.Builder code = CodeBlock.builder();
            addGeneratedBy(code);
            // For each parameter, we need to convert it to the bare shimType if needed.
            for (var parameter : getParameters()) {
                code.add(parameter.toBareVariableDeclaration("_" + parameter.name(), shim));
            }

            // Uni<Map<K, S>> method(...) {
            //  ... params...
            //  Supplier<Future<Map<K, V>>> _res = () -> getDelegate().method(_param1, _param2);
            //  return Uni.createFrom().completionStage(() -> _res.get().toCompletionStage()).map(map -> map.entrySet().stream()
            //       .collect(Collectors.toMap(Map.Entry::getKey, e -> new S(e.getValue())));
            // }

            // Invoke the method
            if (isStatic()) {
                code.addStatement("$T<$T> _res = () -> $T.$L($L)", Supplier.class, futureTypeName,
                        JavaType.of(shim.getSource().getFullyQualifiedName()).toTypeName(),
                        getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            } else {
                code.addStatement("$T<$T> _res = () -> getDelegate().$L($L)", Supplier.class, futureTypeName, getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            }
            code.addStatement(
                    "return $T.createFrom().completionStage(() -> _res.get().toCompletionStage()).map(__map -> __map.entrySet().stream()\n"
                            +
                            "        .collect($T.toMap($T::getKey, e -> new $T(e.getValue()))))",
                    Uni.class,
                    Collectors.class,
                    Map.Entry.class,
                    shim.getVertxGen(originalValueType).concrete() ? shimValueTypeName
                            : JavaType.of(shim.getVertxGen(originalValueType).getShimCompanionName()).toTypeName());

            method.addCode(code.build());
            builder.addMethod(method.build());

            // Extra methods
            Type collectionType = shim.convert(TypeUtils.getFirstParameterizedType(getOriginalMethod().getReturnedType()));
            builder.addMethod(generateAwaitMethod(shim, this, collectionType, getOriginalMethod().getJavadoc(shim)).build());
            builder.addMethod(generateForgetMethod(shim, this, collectionType, getOriginalMethod().getJavadoc(shim)).build());
        }
    }

    /**
     * The original method is returning a {@code Future<X>} where {@code X} is not a {@code Vert.x Gen} object.
     * We just delegate to the original method and wrap the result into a {@code Uni}:
     * <code>
     * <pre>
     * Uni&lt;X&gt method(...) {
     *     ... params...
     *     Future&lt;X&gt; _res = getDelegate().method(_param1, _param2);
     *     return Uni.createFrom().completionStage(() -> _res.toCompletionStage());
     * }
     * </pre>
     * </code>
     */
    private static class UniDelegatingMethod extends BaseShimMethod {

        private final Type shimElementType;
        private final Javadoc originalJavadoc;

        public UniDelegatingMethod(ShimModule module, ShimClass shim, VertxGenMethod method, boolean publisherSignature) {
            super(
                    module,
                    method.getName(),
                    TypeUtils.convertBareToShimReturnType(shim, method),
                    TypeUtils.convertBareToShimParameters(shim, method, publisherSignature),
                    TypeUtils.convertBaseToShimThrows(shim, method),
                    method.isStatic(),
                    method.isFinal(),
                    adaptJavadocToUni(shim, method),
                    method);

            ResolvedType originalReturnType = getOriginalMethod().getReturnedType();
            ResolvedType paramType = TypeUtils.getFirstParameterizedType(originalReturnType);
            this.shimElementType = shim.convert(paramType);
            this.originalJavadoc = method.getJavadoc(shim);
            setJavadoc(adaptParameterJavadoc(shim, getJavadoc(), getParameters()));
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            var futureTypeName = JavaType.of(getOriginalMethod().getReturnedType().describe()).toTypeName();

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
            // ... params ...
            // Supplier<Future<X>> _res = () -> getDelegate().method(_param1, _param2);
            // return Uni.createFrom().completionStage(() -> _res.get().toCompletionStage());

            if (isStatic()) {
                code.addStatement("$T<$T> _res = () -> $T.$L($L)", Supplier.class, futureTypeName,
                        JavaType.of(shim.getSource().getFullyQualifiedName()).toTypeName(),
                        getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            } else {
                code.addStatement("$T<$T> _res = () -> getDelegate().$L($L)", Supplier.class, futureTypeName, getName(),
                        String.join(", ", getParameters().stream().map(p -> "_" + p.name()).toList()));
            }

            code.addStatement("return $T.createFrom().completionStage(() -> _res.get().toCompletionStage())",
                    Uni.class);

            method.addCode(code.build());
            builder.addMethod(method.build());

            // Extra methods
            builder.addMethod(generateAwaitMethod(shim, this, shimElementType, originalJavadoc).build());
            builder.addMethod(generateForgetMethod(shim, this, shimElementType, originalJavadoc).build());
        }
    }

    private static MethodSpec.Builder generateAwaitMethod(ShimClass shim, BaseShimMethod method, Type shimElementType,
            Javadoc originalJavadoc) {
        MethodSpec.Builder awaitMethod = MethodSpec.methodBuilder(method.getName() + "AndAwait");
        awaitMethod.addModifiers(Modifier.PUBLIC);
        if (method.isStatic()) {
            awaitMethod.addModifiers(Modifier.STATIC);
        }
        boolean isVoid = (shimElementType.isVoidType() || shimElementType.asString().equals(Void.class.getName()));
        awaitMethod.returns(isVoid ? TypeName.VOID : Shim.getTypeNameFromType(shimElementType));

        Javadoc awaitDoc = adaptJavadocToAndAwait(shim, shimElementType, method.getOriginalMethod());
        awaitMethod.addJavadoc(awaitDoc.toText());
        for (var parameter : method.getParameters()) {
            awaitMethod.addParameter(Shim.getTypeNameFromType(parameter.shimType()), parameter.name());
        }
        for (var exception : method.getThrows()) {
            awaitMethod.addException(Shim.getTypeNameFromType(exception));
        }
        for (TypeParameter tp : method.getOriginalMethod().getMethod().getTypeParameters()) {
            awaitMethod.addTypeVariable(TypeVariableName.get(tp.getName().asString()));
        }
        CodeBlock.Builder awaitCode = CodeBlock.builder();
        method.addGeneratedBy(awaitCode);
        List<String> callArgs = method.getParameters().stream().map(ShimMethodParameter::name).toList();
        TypeName uniType = Shim.getTypeNameFromType(method.getReturnType());
        awaitCode.addStatement("$T _res = $L($L)", uniType, method.getName(), String.join(", ", callArgs));
        if (!isVoid) {
            awaitCode.addStatement("return _res.await().indefinitely()");
        } else {
            awaitCode.addStatement("_res.await().indefinitely()");
        }
        awaitMethod.addCode(awaitCode.build());
        return awaitMethod;
    }

    private static MethodSpec.Builder generateForgetMethod(ShimClass shim, BaseShimMethod method, Type shimElementType,
            Javadoc originalJavadoc) {
        MethodSpec.Builder forgetMethod = MethodSpec.methodBuilder(method.getName() + "AndForget");
        forgetMethod.addModifiers(Modifier.PUBLIC);
        if (method.isStatic()) {
            forgetMethod.addModifiers(Modifier.STATIC);
        }

        // To avoid compatibility issue with Vert.x 4, consider all non-static andForget method fluent:

        if (!method.isStatic()) {
            forgetMethod.returns(JavaType.of(TypeDescriber.safeDescribeType(shim.getType())).toTypeName());
        } else {
            forgetMethod.returns(TypeName.VOID);
        }

        Javadoc forgetDoc = adaptJavadocToAndForget(shim, shimElementType, method.getOriginalMethod());

        forgetMethod.addJavadoc(forgetDoc.toText());
        for (var parameter : method.getParameters()) {
            forgetMethod.addParameter(Shim.getTypeNameFromType(parameter.shimType()), parameter.name());
        }
        for (var exception : method.getThrows()) {
            forgetMethod.addException(Shim.getTypeNameFromType(exception));
        }
        for (TypeParameter tp : method.getOriginalMethod().getMethod().getTypeParameters()) {
            forgetMethod.addTypeVariable(TypeVariableName.get(tp.getName().asString()));
        }
        CodeBlock.Builder forgetCode = CodeBlock.builder();
        method.addGeneratedBy(forgetCode);
        List<String> callArgs = method.getParameters().stream().map(ShimMethodParameter::name).toList();
        forgetCode.addStatement("$L($L).subscribe().with(_ignored -> {})", method.getName(), String.join(", ", callArgs));

        if (!method.isStatic()) {
            forgetCode.addStatement("return this");
        }

        forgetMethod.addCode(forgetCode.build());
        return forgetMethod;
    }
}
