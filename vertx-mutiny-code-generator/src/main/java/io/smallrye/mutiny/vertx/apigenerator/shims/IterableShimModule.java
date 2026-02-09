package io.smallrye.mutiny.vertx.apigenerator.shims;

import java.util.List;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;
import io.smallrye.mutiny.vertx.apigenerator.types.JavaType;
import io.smallrye.mutiny.vertx.apigenerator.types.ResolvedTypeDescriber;
import io.smallrye.mutiny.vertx.apigenerator.types.TypeDescriber;
import io.smallrye.mutiny.vertx.apigenerator.utils.TypeUtils;
import io.smallrye.mutiny.vertx.impl.MappingIterator;

/**
 * A shim module checking if the source implement the {@code Iterable} interface and if so, adds the corresponding interface
 * and method to the shim class.
 */
public class IterableShimModule implements ShimModule {

    public static final String ITERABLE = Iterable.class.getName();

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
            if (reference.getQualifiedName().equalsIgnoreCase(ITERABLE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void analyze(ShimClass shim) {
        NodeList<ClassOrInterfaceType> extendedTypes = shim.getSource().getDeclaration().getExtendedTypes();
        ResolvedType typeOfItems = null;
        Type converted = null;
        boolean isItemVertxGen = false;
        for (ClassOrInterfaceType type : extendedTypes) {
            ResolvedReferenceType reference = type.resolve().asReferenceType();
            if (reference.getQualifiedName().equalsIgnoreCase(ITERABLE)) {
                typeOfItems = TypeUtils.getFirstParameterizedType(reference);
                converted = shim.getSource().getGenerator().getConverters().convert(typeOfItems);
                if (shim.isVertxGen(typeOfItems)) {
                    isItemVertxGen = true;
                }
                shim.addInterface(StaticJavaParser.parseClassOrInterfaceType(ITERABLE).setTypeArguments(converted));
            }
        }

        if (typeOfItems == null) {
            return;
        }

        shim.addMethod(new IteratorMethod(this, typeOfItems, converted, isItemVertxGen));
        shim.addMethod(new ToMultiMethod(this, converted));
    }

    public static class IteratorMethod extends BaseShimMethod {

        private final boolean isItemVertxGen;
        private final Type convertedTypeOfItem;
        private final ResolvedType originalTypeOfItem;

        public IteratorMethod(ShimModule module, ResolvedType originalTypeOfItem, Type convertedTypeOfItem,
                boolean isItemVertxGen) {
            super(
                    module,
                    "iterator",
                    StaticJavaParser.parseClassOrInterfaceType("java.util.Iterator").setTypeArguments(convertedTypeOfItem),
                    List.of(),
                    List.of(),
                    false,
                    false,
                    null, // TODO Javadoc.
                    null);
            setOverridden(true);
            this.isItemVertxGen = isItemVertxGen;
            this.convertedTypeOfItem = convertedTypeOfItem;
            this.originalTypeOfItem = originalTypeOfItem;
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            var method = super.generateDeclaration(shim, builder);
            super.addGeneratedBy(method);
            if (isItemVertxGen) {
                // Plain -> Shim
                method.addStatement("java.util.function.Function<$T, $T> conv = $T::newInstance",
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalTypeOfItem)).toTypeName(),
                        JavaType.of(TypeDescriber.safeDescribeType(convertedTypeOfItem)).toTypeName(),
                        JavaType.of(TypeDescriber.safeDescribeType(convertedTypeOfItem)).toTypeName());
                method.addStatement("return new $T<>(delegate.iterator(), conv)", MappingIterator.class);
            } else {
                method.addStatement("return delegate.iterator()");
            }
            builder.addMethod(method.build());
        }
    }

    /**
     * The `toMulti` method:
     * <p>
     *
     * <pre>{@code
     * @CheckReturnValue
     * public Multi<E> iterator() {
     *     return Multi.createFrom().iterable(this);
     * }
     * }</pre>
     */
    public static class ToMultiMethod extends BaseShimMethod {

        public ToMultiMethod(ShimModule module, Type elementType) {
            super(
                    module,
                    "toMulti",
                    StaticJavaParser.parseClassOrInterfaceType("io.smallrye.mutiny.Multi").setTypeArguments(elementType),
                    List.of(),
                    List.of(),
                    false,
                    false,
                    null,
                    null);
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = super.generateDeclaration(shim, builder);
            super.addGeneratedBy(method);

            method.addAnnotation(CheckReturnValue.class);
            method.addStatement("return $T.createFrom().iterable(this)", Multi.class);

            builder.addMethod(method.build());
        }
    }

}
