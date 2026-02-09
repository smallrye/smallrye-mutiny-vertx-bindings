package io.smallrye.mutiny.vertx.apigenerator.shims;

import com.github.javaparser.StaticJavaParser;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import io.smallrye.mutiny.vertx.TypeArg;
import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimField;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;

/**
 * A shim module checking if the source implement the {@code TypeArg} field.
 */
public class TypeArgConstantShimModule implements ShimModule {
    @Override
    public boolean accept(ShimClass shim) {
        return shim.isClass();
    }

    @Override
    public void analyze(ShimClass shim) {
        shim.addField(new TypeArgConstantField(shim, this));
    }

    private static class TypeArgConstantField extends BaseShimField {
        private final ShimClass shim;

        public TypeArgConstantField(ShimClass shim, TypeArgConstantShimModule module) {
            super(
                    module,
                    "__TYPE_ARG",
                    StaticJavaParser
                            .parseClassOrInterfaceType(TypeArg.class.getName() + "<" + shim.getFullyQualifiedName() + ">"),
                    true, true, false);
            this.shim = shim;
        }

        // TODO Test with generics

        @Override
        protected void initialize(FieldSpec.Builder f) {
            f.initializer("new $T<>(obj -> new $T(($T) obj), $T::$N)",
                    TypeArg.class, ClassName.bestGuess(shim.getFullyQualifiedName()),
                    ClassName.bestGuess(shim.getSource().getFullyQualifiedName()),
                    ClassName.bestGuess(shim.getFullyQualifiedName()),
                    "getDelegate");
        }
    }
}
