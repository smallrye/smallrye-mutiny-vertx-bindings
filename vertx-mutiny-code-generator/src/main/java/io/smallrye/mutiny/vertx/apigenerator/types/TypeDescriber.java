package io.smallrye.mutiny.vertx.apigenerator.types;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

public class TypeDescriber {

    /**
     * Attempts to resolve the type using the symbol solver.
     * If successful, returns e.g. "com.example.Foo<java.lang.String>".
     * If not resolvable, falls back to a best-effort reconstruction
     * from the AST (which may be partial, e.g. "Foo<String>" with no package).
     */
    public static String safeDescribeType(Type astType) {
        try {
            ResolvedType resolvedType = astType.resolve();
            return resolvedType.describe();
        } catch (Exception e) {
            return describeUnresolvedType(astType);
        }
    }

    /**
     * Builds a string from the AST node, including any scope and type arguments.
     * This does NOT require resolution, but it can only show what's literally in the code.
     * Example: if the user wrote "com.example.Foo<String>", you'll get exactly that.
     */
    private static String describeUnresolvedType(Type astType) {
        // Handle arrays, class/interface types, wildcards, etc.
        if (astType.isArrayType()) {
            return describeUnresolvedType(astType.asArrayType().getComponentType()) + "[]";
        }
        if (astType.isClassOrInterfaceType()) {
            return describeClassOrInterfaceType(astType.asClassOrInterfaceType());
        }
        // For primitives, void, var, union types, intersection types, etc.,
        // .asString() often suffices for a fallback.
        return astType.asString();
    }

    private static String describeClassOrInterfaceType(com.github.javaparser.ast.type.ClassOrInterfaceType cit) {
        // If the code literally says "com.example.Outer.Inner<SomeType>",
        // this scope can be another ClassOrInterfaceType or a simple Name
        String scopePart = "";
        if (cit.getScope().isPresent()) {
            scopePart = describeUnresolvedType(cit.getScope().get()) + ".";
        }

        // Collect any type arguments
        String typeArgs = "";
        if (cit.getTypeArguments().isPresent()) {
            var argList = cit.getTypeArguments().get();
            // Recursively describe each type argument
            String joined = argList.stream()
                    .map(TypeDescriber::describeUnresolvedType)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            typeArgs = "<" + joined + ">";
        }

        // Combine scope + name + type arguments
        return scopePart + cit.getNameAsString() + typeArgs;
    }
}