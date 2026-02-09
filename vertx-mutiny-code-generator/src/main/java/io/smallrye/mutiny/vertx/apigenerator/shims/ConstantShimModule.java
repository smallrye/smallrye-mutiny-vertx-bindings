package io.smallrye.mutiny.vertx.apigenerator.shims;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import com.palantir.javapoet.FieldSpec;

import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimField;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenConstant;

/**
 * Handles constants.
 */
public class ConstantShimModule implements ShimModule {
    @Override
    public boolean accept(ShimClass shim) {
        return true;
    }

    @Override
    public void analyze(ShimClass shim) {
        for (VertxGenConstant constant : shim.getSource().getConstants()) {
            shim.addField(new ConstantField(this, shim, constant));
        }
    }

    public static final class ConstantField extends BaseShimField {

        private final String originalClassName;
        private final ShimClass shim;
        private Type shimElementType;
        private String fqn;
        private boolean isVertxGen = false;
        private String vertxGenType;

        private boolean isList;
        private boolean isSet;
        private boolean isMap;

        public ConstantField(ConstantShimModule module, ShimClass clazz, VertxGenConstant constant) {
            super(module,
                    constant.getName(),
                    clazz.convert(constant.getType()),
                    true,
                    true,
                    constant.getField().accessSpecifier().equals(AccessSpecifier.PRIVATE));
            setJavadoc(constant.getJavadoc());
            originalClassName = clazz.getSource().getFullyQualifiedName();
            shim = clazz;

            if (constant.getType().isPrimitive()) {
                return;
            }

            fqn = constant.getType().asReferenceType().erasure().asReferenceType().getQualifiedName();

            if (clazz.getSource().getGenerator().getCollectionResult().isVertxGen(fqn)) {
                isVertxGen = true;
                vertxGenType = fqn;
            } else if (fqn.equals(List.class.getName())) {
                isList = true;
                ResolvedType first = constant.getType().asReferenceType().typeParametersMap().getTypes().get(0);
                isVertxGen = clazz.getSource().getGenerator().getCollectionResult().isVertxGen(first.erasure().describe());
                vertxGenType = first.erasure().describe();
                shimElementType = clazz.convert(first);
            } else if (fqn.equals(Set.class.getName())) {
                isSet = true;
                ResolvedType first = constant.getType().asReferenceType().typeParametersMap().getTypes().get(0);
                isVertxGen = clazz.getSource().getGenerator().getCollectionResult().isVertxGen(first.erasure().describe());
                vertxGenType = first.erasure().describe();
                shimElementType = clazz.convert(first);
            } else if (fqn.equals(Map.class.getName())) {
                isMap = true;
                ResolvedType valueType = constant.getType().asReferenceType().typeParametersMap().getTypes().get(1);
                isVertxGen = clazz.getSource().getGenerator().getCollectionResult().isVertxGen(valueType.erasure().describe());
                vertxGenType = valueType.erasure().describe();
                shimElementType = clazz.convert(valueType);
            }

        }

        @Override
        protected void initialize(FieldSpec.Builder f) {
            String code;
            if (isList) {
                if (isVertxGen) {
                    code = "%s.%s.stream().map(item -> %s.newInstance((%s)item)).collect(java.util.stream.Collectors.toList());"
                            .formatted(originalClassName, getName(), shimElementType, vertxGenType);
                } else {
                    code = "%s.%s".formatted(originalClassName, getName());
                }
            } else if (isMap) {
                if (isVertxGen) {
                    code = "%s.%s.entrySet().stream().collect(java.util.stream.Collectors.toMap(_e -> _e.getKey(), _e -> %s.newInstance((%s) _e.getValue())));"
                            .formatted(originalClassName, getName(), shimElementType, vertxGenType);
                } else {
                    code = "%s.%s".formatted(originalClassName, getName());
                }
            } else if (isSet) {
                if (isVertxGen) {
                    code = "%s.%s.stream().map(item -> %s.newInstance((%s)item)).collect(java.util.stream.Collectors.toSet());"
                            .formatted(originalClassName, getName(), shimElementType, vertxGenType);
                } else {
                    code = "%s.%s".formatted(originalClassName, getName());
                }
            } else if (isVertxGen) {
                var type = getType().asClassOrInterfaceType().getNameWithScope();
                code = "%s.newInstance((%s) %s.%s)".formatted(type, vertxGenType, originalClassName, getName());
            } else {
                code = "%s.%s".formatted(originalClassName, getName());
            }
            f.initializer(code);
        }
    }
}
