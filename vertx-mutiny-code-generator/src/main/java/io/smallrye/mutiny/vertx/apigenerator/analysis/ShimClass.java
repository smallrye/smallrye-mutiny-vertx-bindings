package io.smallrye.mutiny.vertx.apigenerator.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.resolution.types.ResolvedType;

import io.smallrye.mutiny.vertx.apigenerator.ShimConstants;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenClass;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenInterface;

/**
 * Represents a shim class.
 */
public class ShimClass {

    protected final VertxGenInterface source;
    protected final Set<Type> interfaces = new HashSet<>();
    protected final List<ShimMethod> methods = new ArrayList<>();
    protected final List<ShimField> fields = new ArrayList<>();
    protected final List<ShimConstructor> constructors = new ArrayList<>();
    private ShimCompanionClass companion;
    private Type parentClass;

    public ShimClass(VertxGenInterface source) {
        this.source = source;
    }

    /**
     * A utility method to convert the given {@link ResolvedType} to a {@link Type} used in the shim.
     *
     * @param type the shimType, must not be {@code null}
     * @return the converted shimType
     */
    public Type convert(ResolvedType type) {
        return source.getGenerator().convertType(type);
    }

    public String getPackage() {
        return ShimConstants.getPackageName(this.source.getModule(), this.source.getPackageName());
    }

    public String getFullyQualifiedName() {
        return ShimConstants.getClassName(this.source.getModule(), this.source.getFullyQualifiedName());
    }

    public String getSimpleName() {
        return getFullyQualifiedName().substring(getPackage().length() + 1);
    }

    public Javadoc getJavaDoc() {
        if (source.getJavadoc() != null) {
            String newContent = """
                    %s
                    <p>
                    <strong>NOTE:</strong> This class has been automatically generated from the {@link %s  original} non Mutiny-ified interface.
                    </p>
                    """
                    .formatted(source.getJavadoc().getDescription().toText(), source.getFullyQualifiedName());
            Javadoc copy = new Javadoc(JavadocDescription.parseText(newContent));
            for (JavadocBlockTag tag : source.getJavadoc().getBlockTags()) {
                copy.addBlockTag(tag);
            }
            copy.addBlockTag(new JavadocBlockTag(JavadocBlockTag.Type.SEE, source.getFullyQualifiedName()));
            return copy;
        } else {
            return new Javadoc(JavadocDescription.parseText(
                    """
                            <strong>NOTE:</strong> This class has been automatically generated from the {@link %s  original} non Mutiny-ified interface.
                            """
                            .formatted(source.getFullyQualifiedName())))
                    .addBlockTag(new JavadocBlockTag(JavadocBlockTag.Type.SEE, source.getFullyQualifiedName()));
        }
    }

    public boolean isInterface() {
        return !source.isConcrete();
    }

    public boolean isClass() {
        return source.isConcrete();
    }

    public VertxGenInterface getSource() {
        return source;
    }

    public Type getParentClass() {
        return parentClass;
    }

    public Type getType() {
        ClassOrInterfaceType type = StaticJavaParser.parseClassOrInterfaceType(getFullyQualifiedName());
        if (!getSource().getDeclaration().getTypeParameters().isEmpty()) {
            Type[] types = new Type[getSource().getDeclaration().getTypeParameters().size()];
            for (int i = 0; i < getSource().getDeclaration().getTypeParameters().size(); i++) {
                TypeParameter tp = getSource().getDeclaration().getTypeParameters().get(i);
                if (tp.isTypeParameter()) {
                    types[i] = tp;
                } else if (tp.isReferenceType()) {
                    types[i] = StaticJavaParser.parseType(tp.resolve().qualifiedName());
                }
            }
            if (types.length > 0) {
                type = type.setTypeArguments(types);
            }
        }
        return type;
    }

    public Set<Type> getInterfaces() {
        return interfaces;
    }

    public List<ShimMethod> getMethods() {
        return methods;
    }

    public List<ShimField> getFields() {
        return fields;
    }

    public void addInterface(Type type) {
        interfaces.add(type);
    }

    public void addMethod(ShimMethod method) {
        // TODO Would be great to verify we do not have conflicts.
        methods.add(method);
    }

    public void addField(ShimField field) {
        fields.add(field);
    }

    public void addConstructor(ShimConstructor constructor) {
        constructors.add(constructor);
    }

    public List<ShimConstructor> getConstructors() {
        return constructors;
    }

    public boolean isVertxGen(ResolvedType type) {
        if (type.isReferenceType()) {
            return isVertxGen(type.asReferenceType().getQualifiedName());
        }
        return false;
    }

    public void addCompanionClass(ShimCompanionClass companion) {
        this.companion = companion;
    }

    public ShimCompanionClass getCompanion() {
        return companion;
    }

    public VertxGenClass getVertxGen(ResolvedType type) {
        if (type.isReferenceType()) {
            return getVertxGen(type.asReferenceType().getQualifiedName());
        }
        return null;
    }

    public void setParentClass(Type itf) {
        if (parentClass == null) {
            parentClass = itf;
        } else {
            throw new IllegalStateException("Multiple parent classes detected for " + source.getFullyQualifiedName() + " : "
                    + parentClass + " and " + itf);
        }
    }

    public boolean isVertxGen(String qualifiedName) {
        return getSource().getGenerator().getCollectionResult().isVertxGen(qualifiedName);
    }

    public VertxGenClass getVertxGen(String qualifiedName) {
        return getSource().getGenerator().getCollectionResult().getVertxGenClass(qualifiedName);
    }

    public boolean isDeprecated() {
        return getSource().isDeprecated();
    }
}
