package io.smallrye.mutiny.vertx.apigenerator.shims;

import java.util.List;
import java.util.function.Function;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimMethodParameter;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;

/**
 * A shim module checking if the source implement the {@code Function} interface and if so, adds the corresponding interface
 * and method to the shim class.
 */
public class FunctionShimModule implements ShimModule {

    public static final String FUNCTION_CLASS_NAME = Function.class.getName();

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
            if (reference.getQualifiedName().equalsIgnoreCase(FUNCTION_CLASS_NAME)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void analyze(ShimClass shim) {
        NodeList<ClassOrInterfaceType> extendedTypes = shim.getSource().getDeclaration().getExtendedTypes();
        ResolvedType originalInputType;
        ResolvedType originalOutputType;
        for (ClassOrInterfaceType type : extendedTypes) {
            ResolvedReferenceType reference = type.resolve().asReferenceType();
            if (reference.getQualifiedName().equalsIgnoreCase(FUNCTION_CLASS_NAME)) {
                originalInputType = reference.asReferenceType().getTypeParametersMap().get(0).b;
                originalOutputType = reference.asReferenceType().getTypeParametersMap().get(1).b;

                var input = shim.getSource().getGenerator().getConverters().convert(originalInputType);
                var output = shim.getSource().getGenerator().getConverters().convert(originalOutputType);
                shim.addInterface(
                        StaticJavaParser.parseClassOrInterfaceType(FUNCTION_CLASS_NAME).setTypeArguments(input, output));
                shim.addMethod(new ApplyMethod(this, shim, originalInputType, originalOutputType));
            }
        }

    }

    /**
     * The `apply` method:
     *
     * <pre>
     *     @Override
     *     public O apply(I in) {
     *         // Step 1 - Invoke the delegate method
     *         var ret;
     *         // If the input is a Vert.x Gen shimType
     *         ret = getDelegate().apply(in.getDelegate());
     *
     *         // Or if the input is a shimType variable
     *         java.util.function.Function<I, I> inConv = (java.util.function.Function<T, T>) __typeArg_0.unwrap;
     *         ret = getDelegate().apply(inConv.apply);
     *
     *         // Otherwise
     *         ret = getDelegate().apply(in);
     *
     *
     *         // Step 2 - Convert the output if needed
     *         // If the output is a Vert.x Gen shimType
     *         return O.newInstance(ret);
     *
     *         // Or if the output is a shimType variable
     *         java.util.function.Function<O, O> outConv = (java.util.function.Function<O, O>) __typeArg_1.wrap
     *         return outConv.apply(ret);
     *
     *         // Otherwise
     *         return ret;
     *     }
     *
     * </pre>
     */
    public static class ApplyMethod extends BaseShimMethod {

        public ApplyMethod(ShimModule module, ShimClass shim, ResolvedType originItemType, ResolvedType originOutputType) {
            super(
                    module,
                    "apply",
                    shim.convert(originOutputType),
                    List.of(new ShimMethodParameter("item", shim.convert(originItemType), originItemType, false)),
                    List.of(),
                    false, false, null, null);
        }

        // TODO: Implement the method generation
    }

}
