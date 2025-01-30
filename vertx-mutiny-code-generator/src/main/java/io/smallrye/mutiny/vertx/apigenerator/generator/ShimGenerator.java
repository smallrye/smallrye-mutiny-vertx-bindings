package io.smallrye.mutiny.vertx.apigenerator.generator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import io.smallrye.mutiny.vertx.MutinyGen;
import io.smallrye.mutiny.vertx.apigenerator.analysis.Shim;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimCompanionClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimConstructor;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimField;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.types.JavaType;
import io.smallrye.mutiny.vertx.apigenerator.types.TypeDescriber;

public class ShimGenerator {

    private final ShimClass shim;
    private static final Logger logger = LoggerFactory.getLogger(ShimGenerator.class);

    public ShimGenerator(ShimClass shim) {
        this.shim = shim;
    }

    public JavaFile generate() {
        logger.info("Generating {}", shim.getFullyQualifiedName());

        TypeSpec.Builder builder;
        ClassName className = ClassName.get(shim.getPackage(), shim.getSimpleName());
        if (shim.getSource().isConcrete()) {
            builder = TypeSpec.classBuilder(className);
            if (shim.getParentClass() != null) {
                builder.superclass(Shim.getTypeNameFromType(shim.getParentClass()));
            }
        } else {
            builder = TypeSpec.interfaceBuilder(className);
        }

        Set<String> interfaces = new HashSet<>();
        for (Type itf : shim.getInterfaces()) {
            String val = TypeDescriber.safeDescribeType(itf);
            if (interfaces.add(val)) {
                builder.addSuperinterface(JavaType.of(val).toTypeName());
            }
        }

        builder.addModifiers(Modifier.PUBLIC)
                .addJavadoc(sanitize(shim.getJavaDoc().toText()))
                .addAnnotation(AnnotationSpec.builder(MutinyGen.class)
                        .addMember("value", shim.getSource().getFullyQualifiedName() + ".class").build());

        for (TypeParameter parameter : shim.getSource().getTypeParameters()) {
            builder.addTypeVariable(TypeVariableName.get(parameter.getNameAsString()));
        }

        List<ShimField> fields = shim.getFields();
        for (ShimField field : fields) {
            field.generate(shim, builder);
        }

        if (shim.getSource().isConcrete()) {
            for (ShimConstructor constructor : shim.getConstructors()) {
                constructor.generate(shim, builder);
            }
            for (ShimMethod method : shim.getMethods()) {
                method.generate(shim, builder);
            }
        } else {
            for (ShimMethod method : shim.getMethods()) {
                if (method.isStatic()) {
                    method.generate(shim, builder);
                } else {
                    // Generate only the declaration for non-static methods
                    // Be aware that generateDeclaration does not add the method to the type
                    // because the method is used by the other methods to generate the method body
                    builder.addMethod(method.generateDeclaration(shim, builder).build());
                }
            }
        }

        // Generate the companion if needed
        if (shim.getCompanion() != null) {
            generateCompanionClass(shim, builder);
        }
        TypeSpec built = builder.build();
        JavaFile.Builder jf = JavaFile.builder(shim.getPackage(), built)
                .skipJavaLangImports(true);

        return jf.build();
    }

    private String sanitize(String text) {
        // TODO Check javadoc from io.vertx.core.http.WebSocketFrame
        return text
                .replace("$", "$$");
    }

    /**
     * Generates the companion class, i.e. a package private class in the same file as the main class.
     * It implements the shim interface.
     *
     * @param shim the shim class
     * @param builder the builder
     */
    private void generateCompanionClass(ShimClass shim, TypeSpec.Builder builder) {
        ShimCompanionClass companion = shim.getCompanion();
        logger.info("Generating companion class {} (simple name: {})",
                companion.getFullyQualifiedName(), companion.getSimpleName());
        TypeSpec.Builder companionBuilder = TypeSpec.classBuilder(companion.getSimpleName())
                .addSuperinterface(Shim.getTypeNameFromType(shim.getType()))
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC);

        for (TypeParameter parameter : shim.getSource().getTypeParameters()) {
            companionBuilder.addTypeVariable(TypeVariableName.get(parameter.getNameAsString()));
        }

        for (ShimField field : companion.getFields()) {
            field.generate(companion, companionBuilder);
        }

        for (ShimConstructor constructor : companion.getConstructors()) {
            constructor.generate(companion, companionBuilder);
        }

        for (ShimMethod method : companion.getMethods()) {
            method.generate(companion, companionBuilder);
        }

        builder.addType(companionBuilder.build());
    }

}
