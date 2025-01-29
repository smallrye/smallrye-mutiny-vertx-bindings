package io.smallrye.mutiny.vertx.apigenerator.shims;

import java.util.Iterator;
import java.util.List;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;

/**
 * A shim module checking if the source implement the {@code Iterator} interface and if so, adds the corresponding interface
 * and method to the shim class.
 */
public class IteratorShimModule implements ShimModule {

    public static final String ITERATOR = Iterator.class.getName();

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
            if (reference.getQualifiedName().equalsIgnoreCase(ITERATOR)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void analyze(ShimClass shim) {
        NodeList<ClassOrInterfaceType> extendedTypes = shim.getSource().getDeclaration().getExtendedTypes();
        Type converted = null;
        ResolvedType elementType;
        for (ClassOrInterfaceType type : extendedTypes) {
            ResolvedReferenceType reference = type.resolve().asReferenceType();
            if (reference.getQualifiedName().equalsIgnoreCase(ITERATOR)) {
                elementType = reference.asReferenceType().typeParametersMap().getTypes().getFirst();
                converted = shim.getSource().getGenerator().getConverters().convert(elementType);
                shim.addInterface(StaticJavaParser.parseClassOrInterfaceType(ITERATOR).setTypeArguments(converted));
            }
        }

        shim.addMethod(new HasNextMethod(this));
        shim.addMethod(new NextMethod(this, converted));
        shim.addMethod(new ToMultiMethod(this, converted));
    }

    /**
     * The `hasNext` method.
     * It should just delegate to the `delegate` field:
     *
     * <pre>
     * @Override
     * public boolean hasNext() {
     *     return delegate.hasNext();
     * }
     * </pre>
     */
    public static final class HasNextMethod extends BaseShimMethod {

        public HasNextMethod(ShimModule module) {
            super(
                    module,
                    "hasNext",
                    StaticJavaParser.parseType("boolean"),
                    List.of(),
                    List.of(),
                    false,
                    false,
                    null,
                    null);
        }
    }

    /**
     * The `next` method.
     * Depending on the shimType, it must be converted:
     *
     * <pre>
     * public X next() {
     *     return X.newInstance(delegate.next()); // If Vert.x Gen
     *     // or
     *     return __typeArg_0.wrap(delegate.next()); // If shimType variable
     *     // or
     *     return delegate.next(); // Otherwise
     * }
     * </pre>
     */
    public static final class NextMethod extends BaseShimMethod {

        public NextMethod(ShimModule module, Type returnType) {
            super(
                    module,
                    "next",
                    returnType,
                    List.of(),
                    List.of(),
                    false,
                    false,
                    null,
                    null);
        }

        // TODO Implement the generate method
    }

    /**
     * The `toMulti` method:
     * <p>
     *
     * <pre>
     *  * <code>
     *  *  *  *  *   * &#064;CheckReturnValue
           * public Multi<X> toMulti() {
           *     String support = StreamSupport.class.getName();
           *     String splitIterators = Spliterators.class.getName() + ".spliteratorUnknownSize";
           *     String ordered = Spliterator.class.getName() + ".ORDERED";
           *     return Multi.createFrom().items("
           *        StreamSupport.stream(SplitIterators.splitIteratorUnknownSize(this, ORDERED), false));
           * </pre>
           * </code>
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
    }

}
