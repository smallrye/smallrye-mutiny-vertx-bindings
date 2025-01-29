package io.smallrye.mutiny.vertx.apigenerator.collection;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

public class VertxGenConstant {
    private final ResolvedFieldDeclaration field;
    private final FieldDeclaration declaration;
    private final Javadoc javadoc;

    public VertxGenConstant(FieldDeclaration field) {
        this.declaration = field;
        this.javadoc = field.getJavadoc().orElse(null);
        this.field = field.resolve();
    }

    public String getName() {
        return field.getName();
    }

    public ResolvedType getType() {
        return field.getType();
    }

    public FieldDeclaration getDeclaration() {
        return declaration;
    }

    public Javadoc getJavadoc() {
        return javadoc;
    }

    public ResolvedFieldDeclaration getField() {
        return field;
    }

}
