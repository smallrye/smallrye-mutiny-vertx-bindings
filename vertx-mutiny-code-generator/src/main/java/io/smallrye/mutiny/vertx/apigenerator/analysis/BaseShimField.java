package io.smallrye.mutiny.vertx.apigenerator.analysis;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;
import com.palantir.javapoet.*;

import io.smallrye.mutiny.vertx.apigenerator.types.JavaType;

public class BaseShimField implements ShimField {

    private final String name;
    private final Type type;

    private final boolean isStatic;

    private final boolean isFinal;
    private final ShimModule module;
    private final boolean isPrivate;

    private Javadoc javadoc;

    public BaseShimField(ShimModule module, String name, Type type, boolean isStatic, boolean isFinal, boolean isPrivate) {
        this.name = name;
        this.type = type;
        this.isStatic = isStatic;
        this.isFinal = isFinal;
        this.module = module;
        this.isPrivate = isPrivate;
    }

    public BaseShimField(ShimModule module, String name, Type type, boolean isStatic, boolean isFinal) {
        this(module, name, type, isStatic, isFinal, false);
    }

    public BaseShimField(ShimModule module, String name, Type type, boolean isFinal) {
        this(module, name, type, false, isFinal);
    }

    @Override
    public ShimModule declaredBy() {
        return module;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public boolean isFinal() {
        return isFinal;
    }

    @Override
    public void generate(ShimClass shim, TypeSpec.Builder builder) {
        TypeName cn;
        if (type.isClassOrInterfaceType() && type.asClassOrInterfaceType().getTypeArguments().isPresent()) {
            var raw = ClassName.bestGuess(type.asClassOrInterfaceType().getNameWithScope());
            List<TypeName> typeArguments = new ArrayList<>();
            type.asClassOrInterfaceType().getTypeArguments().get().forEach(t -> {
                if (t.isReferenceType()) {
                    typeArguments.add(JavaType.of(t.asString()).toTypeName());
                }
            });
            cn = ParameterizedTypeName.get(raw, typeArguments.toArray(new TypeName[0]));
        } else if (type.isPrimitiveType()) {
            cn = switch (type.asPrimitiveType().asString()) {
                case "boolean" -> TypeName.BOOLEAN;
                case "byte" -> TypeName.BYTE;
                case "short" -> TypeName.SHORT;
                case "int" -> TypeName.INT;
                case "long" -> TypeName.LONG;
                case "char" -> TypeName.CHAR;
                case "float" -> TypeName.FLOAT;
                case "double" -> TypeName.DOUBLE;
                default -> throw new IllegalStateException("Unexpected value: " + type.asPrimitiveType().asString());
            };
        } else {
            cn = ClassName.bestGuess(type.asString());
        }
        FieldSpec.Builder f = FieldSpec.builder(cn, getName());
        if (isFinal) {
            f.addModifiers(Modifier.FINAL);
        }
        if (isStatic) {
            f.addModifiers(Modifier.STATIC);
        }
        if (isPrivate) {
            f.addModifiers(Modifier.PRIVATE);
        } else {
            f.addModifiers(Modifier.PUBLIC);
        }
        if (this.javadoc != null) {
            f.addJavadoc(this.javadoc.toText());
        }
        initialize(f);
        builder.addField(f.build());
    }

    protected void initialize(FieldSpec.Builder f) {
        // Do nothing by default
    }

    protected BaseShimField setJavadoc(Javadoc javadoc) {
        this.javadoc = javadoc;
        return this;
    }
}
