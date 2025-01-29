package io.smallrye.mutiny.vertx.apigenerator.shims;

import java.util.List;

import javax.lang.model.element.Modifier;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;

import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimField;
import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.Shim;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;

/**
 * A shim module adding the `delegate` field and the `getDelegate()` method
 */
public class DelegateShimModule implements ShimModule {

    private static final String TYPE_ARG = "io.smallrye.mutiny.vertx.TypeArg";

    @Override
    public boolean accept(ShimClass shim) {
        return true;
    }

    @Override
    public void analyze(ShimClass shim) {
        Type type = shim.getSource().getType();
        if (shim.isClass()) {
            shim.addMethod(new DelegateMethod(this, type));
            shim.addField(new DelegateField(this, type));

            // In addition, we need the shimType TypeArg for each shimType parameter
            int index = 0;
            for (TypeParameter typeParam : shim.getSource().getDeclaration().getTypeParameters()) {
                Type t = StaticJavaParser.parseClassOrInterfaceType(TYPE_ARG)
                        .setTypeArguments(StaticJavaParser.parseType(typeParam.getNameAsString()));
                shim.addField(new TypeVarField(this, index, t));
                index++;
            }
        } else {
            shim.addMethod(new DelegateMethodDeclaration(this, shim, type));
        }
    }

    /**
     * Adds the `getDelegate` method
     */
    public static class DelegateMethod extends BaseShimMethod {

        public DelegateMethod(ShimModule module, Type type) {
            super(
                    module,
                    "getDelegate",
                    type,
                    List.of(),
                    List.of(),
                    false, false, null, null);
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = MethodSpec.methodBuilder(getName());
            method.addModifiers(javax.lang.model.element.Modifier.PUBLIC);
            method.returns(Shim.getTypeNameFromType(getReturnType()));
            method.addStatement("return delegate");
            addGetDelegateJavadoc(method);
            builder.addMethod(method.build());
        }
    }

    private static void addGetDelegateJavadoc(MethodSpec.Builder method) {
        method.addJavadoc("""
                Get the <em>delegate</em> instance.
                <p>
                This method returns the instance on which this shim is delegating the calls.
                And so, give you access to the <em>bare</em> API.
                </p>

                @return the delegate instance
                """);
    }

    /**
     * Adds the `getDelegate` method
     */
    public static class DelegateMethodDeclaration extends BaseShimMethod {

        public DelegateMethodDeclaration(ShimModule module, ShimClass shim, Type type) {
            super(
                    module,
                    "getDelegate",
                    shim.getSource().isConcrete() ? type : erased(type),
                    List.of(),
                    List.of(),
                    false, false, null, null);
        }

        private static Type erased(Type type) {
            if (type.isClassOrInterfaceType()) {
                return type.asClassOrInterfaceType().setTypeArguments(new NodeList<>());
            }
            return type;
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = MethodSpec.methodBuilder(getName());
            method.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
            method.returns(Shim.getTypeNameFromType(getReturnType()));
            addGetDelegateJavadoc(method);
            builder.addMethod(method.build());
        }
    }

    /**
     * The delegate field
     *
     * <pre>
     * private final X delegate;
     * // or
     * private final X<T> delegate;
     * </pre>
     */
    public static class DelegateField extends BaseShimField {

        public DelegateField(ShimModule module, Type type) {
            super(module, "delegate", type, false, true, true);
        }
    }

    /**
     * The shimType var fields
     */
    public static class TypeVarField extends BaseShimField {

        public TypeVarField(ShimModule module, int index, Type type) {
            super(module, "__typeArg_" + index, type, false, true, false);
        }
    }

}
