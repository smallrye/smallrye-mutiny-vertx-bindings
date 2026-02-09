package io.smallrye.mutiny.vertx.apigenerator.shims;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.PrimitiveType;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * A shim module adding the `hashCode`, `equals` and `toString` methods to the shim class.
 */
public class EqualsHashCodeAndToStringShimModule implements ShimModule {

    @Override
    public boolean accept(ShimClass shim) {
        return shim.getSource().isConcrete();
    }

    @Override
    public void analyze(ShimClass shim) {
        if (!shim.getSource().hasMethod("hashCode", List.of())) {
            shim.addMethod(new HashCodeMethod(this));
        }
        if (!shim.getSource().hasMethod("equals", List.of("java.lang.Object"))) {
            shim.addMethod(new EqualsMethod(this));
        }
        if (!shim.getSource().hasMethod("toString", List.of())) {
            shim.addMethod(new ToStringMethod(this));
        }
    }

    /**
     * Adds the `hashCode` method
     */
    public static class HashCodeMethod extends BaseShimMethod {

        public HashCodeMethod(ShimModule module) {
            super(
                    module,
                    "hashCode",
                    PrimitiveType.intType(),
                    List.of(),
                    List.of(),
                    false, false, null, null);
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = MethodSpec.methodBuilder(getName());
            method.addAnnotation(Override.class);
            method.addModifiers(Modifier.PUBLIC);
            method.returns(int.class);
            addGeneratedBy(method);
            method.addStatement("return delegate.hashCode()");
            builder.addMethod(method.build());
        }
    }

    /**
     * Adds the `equals` method
     */
    public static class EqualsMethod extends BaseShimMethod {

        public EqualsMethod(ShimModule module) {
            super(
                    module,
                    "equals",
                    PrimitiveType.booleanType(),
                    List.of(),
                    List.of(),
                    false, false, null, null);
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = MethodSpec.methodBuilder(getName());
            method.addAnnotation(Override.class);
            method.addModifiers(Modifier.PUBLIC);
            method.returns(boolean.class);
            method.addParameter(Object.class, "o");
            addGeneratedBy(method);
            method.addStatement("if (this == o) return true;");
            method.addStatement("if (o == null || getClass() != o.getClass()) return false;");
            method.addStatement("$T that = ($T) o",
                    ClassName.bestGuess(shim.getFullyQualifiedName()),
                    ClassName.bestGuess(shim.getFullyQualifiedName()));
            method.addStatement("return delegate.equals(that.getDelegate())");
            builder.addMethod(method.build());
        }
    }

    /**
     * Adds the `hashCode` method
     */
    public static class ToStringMethod extends BaseShimMethod {

        public ToStringMethod(ShimModule module) {
            super(
                    module,
                    "toString",
                    StaticJavaParser.parseClassOrInterfaceType(String.class.getName()),
                    List.of(),
                    List.of(),
                    false, false, null, null);
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = MethodSpec.methodBuilder(getName());
            method.addAnnotation(Override.class);
            method.addModifiers(Modifier.PUBLIC);
            method.returns(String.class);
            addGeneratedBy(method);
            method.addStatement("return delegate.toString()");
            builder.addMethod(method.build());
        }
    }

}
