package io.smallrye.mutiny.vertx.apigenerator.shims;

import java.util.List;
import java.util.stream.Stream;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimField;
import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;
import io.smallrye.mutiny.vertx.apigenerator.types.JavaType;
import io.smallrye.mutiny.vertx.apigenerator.types.ResolvedTypeDescriber;
import io.smallrye.mutiny.vertx.apigenerator.types.TypeDescriber;

/**
 * Generate the toMulti, toBlockingIterable and toBlockingStream methods when the source implements the ReadStream interface.
 */
public class ReadStreamModule implements ShimModule {

    public static final String READ_STREAM_CLASS_NAME = "io.vertx.core.streams.ReadStream";

    @Override
    public boolean accept(ShimClass shim) {
        return getOriginalReadStreamType(shim) != null;
    }

    private ResolvedReferenceType getOriginalReadStreamType(ShimClass shim) {
        ClassOrInterfaceDeclaration declaration = shim.getSource().getDeclaration();
        ResolvedReferenceTypeDeclaration resolved = declaration.resolve();
        for (ResolvedReferenceType ancestor : resolved.getAllAncestors()) {
            if (ancestor.getQualifiedName().equals(READ_STREAM_CLASS_NAME)) {
                return ancestor;
            }
        }
        return null;
    }

    @Override
    public void analyze(ShimClass shim) {
        // Extract the type of item emitted by the read stream
        ResolvedReferenceType readStreamType = getOriginalReadStreamType(shim);
        if (readStreamType == null) {
            return;
        }
        ResolvedType originalItemType = readStreamType.typeParametersMap().getTypes().getFirst();
        Type itemType = shim.getSource().getGenerator().getConverters().convert(originalItemType);

        if (shim.isClass()) {
            shim.addField(new MultiField(this, itemType));
            shim.addMethod(new toMultiMethod(this, shim, originalItemType, itemType, false));
            shim.addMethod(new toBlockingIterableMethod(this, itemType));
            shim.addMethod(new toBlockingStreamMethod(this, itemType));
        } else {
            shim.addMethod(new toMultiMethod(this, shim, originalItemType, itemType, true));
        }

    }

    private static class MultiField extends BaseShimField {
        public MultiField(ShimModule module, Type itemType) {
            super(
                    module,
                    "multi",
                    StaticJavaParser.parseClassOrInterfaceType(Multi.class.getName()).setTypeArguments(itemType),
                    false, false, true);
        }
    }

    private static class toMultiMethod extends BaseShimMethod {

        private final boolean isVertxGen;
        private final ResolvedType originalItemType;
        private final Type itemType;
        private final boolean declarationOnly;

        public toMultiMethod(ShimModule module, ShimClass shim, ResolvedType originalItemType, Type itemType,
                boolean declarationOnly) {
            super(
                    module,
                    "toMulti",
                    StaticJavaParser.parseClassOrInterfaceType(Multi.class.getName()).setTypeArguments(itemType),
                    List.of(),
                    List.of(),
                    false,
                    false,
                    null, // TODO - surprisingly, we do not have javadoc for this method in the old generator.
                    null);
            this.isVertxGen = shim.isVertxGen(originalItemType);
            this.originalItemType = originalItemType;
            this.itemType = itemType;
            this.declarationOnly = declarationOnly;
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = super.generateDeclaration(shim, builder);
            method.addAnnotation(CheckReturnValue.class);

            if (declarationOnly) {
                builder.addMethod(method.build());
                return;
            }

            method.addModifiers(javax.lang.model.element.Modifier.SYNCHRONIZED);

            method.beginControlFlow("if (multi == null)");

            // If the element type is Vert.x Gen type, we need to generate a conversion function
            if (isVertxGen) {
                // Generate the conversion function
                // java.util.function.Function<OriginalType, ItemType> conv = OriginalType::newInstance;
                // multi = io.smallrye.mutiny.vertx.MultiHelper.toMulti(delegate, conv);

                method.addStatement("$T<$T, $T> _conv = $T::newInstance", java.util.function.Function.class,
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalItemType)).toTypeName(),
                        JavaType.of(TypeDescriber.safeDescribeType(itemType)).toTypeName(),
                        JavaType.of(itemType.asClassOrInterfaceType().getNameAsString()).toTypeName() // Erased
                );
                method.addStatement("multi = $T.toMulti(delegate, _conv)", io.smallrye.mutiny.vertx.MultiHelper.class);
            } else if (originalItemType.isTypeVariable()) {
                String n = originalItemType.asTypeVariable().describe(); // It should be T
                method.addStatement("$T<$T, $T> _conv = ($T<$T, $T>) __typeArg_0.wrap", // This might be wrong if the type has multiple type parameters.
                        java.util.function.Function.class,
                        TypeVariableName.get(n),
                        TypeVariableName.get(n),
                        java.util.function.Function.class,
                        TypeVariableName.get(n),
                        TypeVariableName.get(n));
                method.addStatement("multi = $T.toMulti(delegate, _conv)", io.smallrye.mutiny.vertx.MultiHelper.class);
            } else {
                method.addStatement("multi = $T.toMulti(this.getDelegate())", io.smallrye.mutiny.vertx.MultiHelper.class);
            }

            method.endControlFlow();
            method.addStatement("return multi");
            builder.addMethod(method.build());
        }
    }

    private static class toBlockingIterableMethod extends BaseShimMethod {

        public toBlockingIterableMethod(ShimModule module, Type itemType) {
            super(
                    module,
                    "toBlockingIterable",
                    StaticJavaParser.parseClassOrInterfaceType(Iterable.class.getName()).setTypeArguments(itemType),
                    List.of(),
                    List.of(),
                    false,
                    false,
                    null, // TODO - surprisingly, we do not have javadoc for this method in the old generator.
                    null);
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = super.generateDeclaration(shim, builder);
            method.addStatement("return toMulti().subscribe().asIterable()");
            builder.addMethod(method.build());
        }
    }

    private static class toBlockingStreamMethod extends BaseShimMethod {

        public toBlockingStreamMethod(ShimModule module, Type itemType) {
            super(
                    module,
                    "toBlockingStream",
                    StaticJavaParser.parseClassOrInterfaceType(Stream.class.getName()).setTypeArguments(itemType),
                    List.of(),
                    List.of(),
                    false,
                    false,
                    null, // TODO - surprisingly, we do not have javadoc for this method in the old generator.
                    null);
        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = super.generateDeclaration(shim, builder);
            method.addStatement("return toMulti().subscribe().asStream()");
            builder.addMethod(method.build());
        }
    }
}
