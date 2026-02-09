package io.smallrye.mutiny.vertx.apigenerator.shims;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedType;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimModule;

/**
 * A shim module that generates the necessary code to handle the hierarchy of the class.
 * Typically, it registers the parent class and interfaces.
 */
public class HierarchyShimModule implements ShimModule {
    @Override
    public boolean accept(ShimClass shim) {
        return true;
    }

    @Override
    public void analyze(ShimClass shim) {
        ClassOrInterfaceDeclaration declaration = shim.getSource().getDeclaration();
        for (ClassOrInterfaceType itf : declaration.getExtendedTypes()) {
            // For each interface, we need to check if it's a Vert.x Gen interface:
            // if not -> Add it to the list of interfaces
            // If yes and if the class is not concrete, add it to the list of interfaces
            // If yes and if the class is concrete, add it to the list of superclasses

            ResolvedType resolved = itf.resolve();
            String fqn = resolved.asReferenceType().getQualifiedName();
            if (shim.getSource().getGenerator().getCollectionResult().isVertxGen(fqn)) {
                if (shim.getSource().getGenerator().getCollectionResult().getVertxGenClass(fqn).concrete()) {
                    shim.setParentClass(shim.convert(resolved));
                } else {
                    shim.addInterface(shim.convert(resolved));
                }
            }
        }
    }
}
