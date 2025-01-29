package io.smallrye.mutiny.vertx.apigenerator.shims;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.stream.Stream;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.vertx.WriteStreamSubscriber;
import io.smallrye.mutiny.vertx.apigenerator.JavadocHelper;
import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimField;
import io.smallrye.mutiny.vertx.apigenerator.analysis.BaseShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;
import io.smallrye.mutiny.vertx.apigenerator.types.JavaType;
import io.smallrye.mutiny.vertx.apigenerator.types.ResolvedTypeDescriber;
import io.smallrye.mutiny.vertx.apigenerator.types.TypeDescriber;

/**
 * Generate the toSubscriber method when the source implements the WriteStream interface.
 */
public class WriteStreamModule implements ShimModule {

    public static final String WRITE_STREAM_CLASS_NAME = "io.vertx.core.streams.WriteStream";

    @Override
    public boolean accept(ShimClass shim) {
        return shim.isClass() && getOriginalWriteStreamType(shim) != null;
    }

    private ResolvedReferenceType getOriginalWriteStreamType(ShimClass shim) {
        ClassOrInterfaceDeclaration declaration = shim.getSource().getDeclaration();
        ResolvedReferenceTypeDeclaration resolved = declaration.resolve();
        for (ResolvedReferenceType ancestor : resolved.getAllAncestors()) {
            if (ancestor.getQualifiedName().equals(WRITE_STREAM_CLASS_NAME)) {
                return ancestor;
            }
        }
        return null;
    }

    @Override
    public void analyze(ShimClass shim) {
        ResolvedReferenceType writeStreamType = getOriginalWriteStreamType(shim);
        if (writeStreamType == null) {
            return;
        }
        ResolvedType originalItemType = writeStreamType.typeParametersMap().getTypes().getFirst();
        Type itemType = shim.getSource().getGenerator().getConverters().convert(originalItemType);

        shim.addField(new SubscriberField(this, itemType));
        shim.addMethod(new toSubscriberMethod(this, shim, originalItemType, itemType));

    }

    private static class SubscriberField extends BaseShimField {
        public SubscriberField(ShimModule module, Type itemType) {
            super(
                    module,
                    "subscriber",
                    StaticJavaParser.parseClassOrInterfaceType(WriteStreamSubscriber.class.getName())
                            .setTypeArguments(itemType),
                    false, false, true);
        }
    }

    private static class toSubscriberMethod extends BaseShimMethod {

        private final boolean isVertxGen;
        private final ResolvedType originalItemType;
        private final Type itemType;

        public toSubscriberMethod(ShimModule module, ShimClass shim, ResolvedType originalItemType, Type itemType) {
            super(
                    module,
                    "toSubscriber",
                    StaticJavaParser.parseClassOrInterfaceType(Flow.Subscriber.class.getName())
                            .setTypeArguments(itemType),
                    List.of(),
                    List.of(),
                    false,
                    false,
                    JavadocHelper.addToJavadoc(null, "Converts the current write stream to a subscriber."),
                    null);
            this.isVertxGen = shim.isVertxGen(originalItemType);
            this.originalItemType = originalItemType;
            this.itemType = itemType;

        }

        @Override
        public void generate(ShimClass shim, TypeSpec.Builder builder) {
            MethodSpec.Builder method = super.generateDeclaration(shim, builder);
            method.addAnnotation(CheckReturnValue.class);

            // Override the return type to avoid the Flow$Subscriber.
            method.returns(ParameterizedTypeName.get(ClassName.get(Flow.Subscriber.class),
                    JavaType.of(TypeDescriber.safeDescribeType(itemType)).toTypeName()));
            method.addModifiers(javax.lang.model.element.Modifier.SYNCHRONIZED);

            method.beginControlFlow("if (subscriber == null)");

            // If the element type is Vert.x Gen type, we need to generate a conversion function
            if (isVertxGen) {
                // Generate the conversion function - from the shim to the original type
                method.addStatement("$T<$T, $T> _conv = $T::getDelegate", java.util.function.Function.class,
                        JavaType.of(TypeDescriber.safeDescribeType(itemType)).toTypeName(),
                        JavaType.of(ResolvedTypeDescriber.describeResolvedType(originalItemType)).toTypeName(),
                        JavaType.of(TypeDescriber.safeDescribeType(itemType)).toTypeName());
                method.addStatement("subscriber = $T.toSubscriber(delegate, _conv)",
                        io.smallrye.mutiny.vertx.MutinyHelper.class);
            } else if (originalItemType.isTypeVariable()) {
                String n = originalItemType.asTypeVariable().describe(); // It should be T
                method.addStatement("$T<$T, $T> _conv = ($T<$T, $T>) __typeArg_0.unwrap",
                        java.util.function.Function.class,
                        TypeVariableName.get(n),
                        TypeVariableName.get(n),
                        java.util.function.Function.class,
                        TypeVariableName.get(n),
                        TypeVariableName.get(n));
                method.addStatement("subscriber = $T.toSubscriber(delegate, _conv)",
                        io.smallrye.mutiny.vertx.MutinyHelper.class);
            } else {
                method.addStatement("subscriber = $T.toSubscriber(this.getDelegate())",
                        io.smallrye.mutiny.vertx.MutinyHelper.class);
            }

            method.endControlFlow();
            method.addStatement("return subscriber");
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
