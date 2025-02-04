package io.smallrye.mutiny.vertx.apigenerator.shims;

import java.util.List;
import java.util.function.Consumer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;

import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimMethodParameter;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;

/**
 * A shim module checking if the source implement the {@code Handler} interface and if so, adds the corresponding interface
 * and method to the shim class.
 */
public class HandlerShimModule implements ShimModule {

    public static final String HANDLER_CLASS_NAME = "io.vertx.core.Handler";

    @Override
    public boolean accept(ShimClass shim) {
        if (!shim.getSource().isConcrete()) {
            return false;
        }
        NodeList<ClassOrInterfaceType> extendedTypes = shim.getSource().getDeclaration().getExtendedTypes();
        if (extendedTypes.isEmpty()) {
            return false;
        }

        for (ClassOrInterfaceType type : extendedTypes) {
            ResolvedReferenceType reference = type.resolve().asReferenceType();
            if (reference.getQualifiedName().equalsIgnoreCase(HANDLER_CLASS_NAME)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void analyze(ShimClass shim) {
        NodeList<ClassOrInterfaceType> extendedTypes = shim.getSource().getDeclaration().getExtendedTypes();
        for (ClassOrInterfaceType type : extendedTypes) {
            ResolvedReferenceType reference = type.resolve().asReferenceType();
            if (reference.getQualifiedName().equalsIgnoreCase(HANDLER_CLASS_NAME)) {
                var elementType = reference.asReferenceType().typeParametersMap().getTypes().get(0);
                var converted = shim.getSource().getGenerator().getConverters().convert(elementType);
                shim.addInterface(StaticJavaParser.parseClassOrInterfaceType(HANDLER_CLASS_NAME).setTypeArguments(converted));
                shim.addInterface(
                        StaticJavaParser.parseClassOrInterfaceType(Consumer.class.getName()).setTypeArguments(converted));
                shim.addMethod(new AcceptMethod(this, shim, elementType));

                // Only add the handle method if the source class does not define it
                if (shim.getSource().getDeclaration().getMethodsByName("handle").isEmpty()) {
                    shim.addMethod(new HandleMethod(this, shim, elementType));
                }
            }
        }

    }

    /**
     * The `handle` method:
     * <p>
     *
     * <pre>{@code
     * @Override
     * public void handle(E event) {
     *     delegate.handle(convertIfNeeded(event));
     * }
     * }</pre>
     *
     * <p>
     * If #isParameterVertxGen() returns true, the body of the method must be:
     * <code>delegate.handle(event.getDelegate())</code>
     * Otherwise, it must be: <code>delegate.handle(event)</code>
     */
    public static class HandleMethod extends BaseShimMethod {

        private final boolean isVertxGen;

        public HandleMethod(ShimModule module, ShimClass shim, ResolvedType originalItemType) {
            super(
                    module,
                    "handle",
                    new VoidType(),
                    List.of(new ShimMethodParameter("item", shim.convert(originalItemType), originalItemType)),
                    null,
                    false,
                    false,
                    null,
                    null);
            setJavadoc(new Javadoc(JavadocDescription.parseText("""
                    Handle an item.
                    This method is generated from the {@link %s#handle original} method."
                    """.formatted(shim.getSource().getFullyQualifiedName()))));
            setOverridden(true);
            isVertxGen = shim.isVertxGen(originalItemType);
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = super.generateDeclaration(shim, builder);

            if (isVertxGen) {
                method.addCode("delegate.handle(item.getDelegate());");
            } else {
                method.addCode("delegate.handle(item);");
            }

            builder.addMethod(method.build());
        }
    }

    /**
     * The `accept` method:
     * <p>
     *
     * <pre>
     * <code>
     * &#064;@Override
     * public void accept(E event) {
     *     handle(event);
     * }
     * </code>
     * </pre>
     */
    public static class AcceptMethod extends BaseShimMethod {

        public AcceptMethod(ShimModule module, ShimClass shim, ResolvedType originalItemType) {
            super(
                    module,
                    "accept",
                    new VoidType(),
                    List.of(new ShimMethodParameter("item", shim.convert(originalItemType), originalItemType)),
                    null,
                    false,
                    false,
                    null,
                    null);
            setJavadoc(new Javadoc(JavadocDescription.parseText("""
                    Handle an item.
                    This method is generated from the {@link %s#accept original} method."
                    """.formatted(shim.getSource().getFullyQualifiedName()))));
            setOverridden(true);
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = generateDeclaration(shim, builder);
            addGeneratedBy(method);

            method.addCode("handle(item);");

            builder.addMethod(method.build());

        }
    }

}
