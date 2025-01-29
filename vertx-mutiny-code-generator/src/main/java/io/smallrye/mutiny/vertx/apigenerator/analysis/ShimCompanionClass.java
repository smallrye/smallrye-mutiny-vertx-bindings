package io.smallrye.mutiny.vertx.apigenerator.analysis;

import java.util.List;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;

import io.smallrye.mutiny.vertx.apigenerator.ShimConstants;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenInterface;

/**
 * Represents a shim companion class, i.e. an `Impl` class associated with a non-concrete shim class.
 */
public class ShimCompanionClass extends ShimClass {

    public ShimCompanionClass(VertxGenInterface source) {
        super(source);
    }

    public String getFullyQualifiedName() {
        return ShimConstants.getClassName(this.source.getModule(), this.source.getFullyQualifiedName())
                + "." + this.source.getSimpleName() + "Impl";
    }

    public String getSimpleName() {
        return this.source.getSimpleName() + "Impl";
    }

    public Javadoc getJavaDoc() {
        return null; // No javadoc in the companion class
    }

    public boolean isInterface() {
        return false;
    }

    public boolean isClass() {
        return true;
    }

    public Type getParentClass() {
        return null; // No parent class for a companion class
    }

    public List<Type> getShimImplementedOrExtendedInterfaces() {
        return List.of(super.getType()); // The companion class implements the shim class (which is an interface in this case)
    }

    @Override
    public void addCompanionClass(ShimCompanionClass companion) {
        throw new UnsupportedOperationException("A companion class cannot have a companion class");
    }
}
