package io.smallrye.mutiny.vertx.apigenerator.types;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedWildcard;

import java.util.stream.Collectors;

public class ResolvedTypeDescriber {

    /**
     * Returns a "fully qualified" string representation of a ResolvedType,
     * including its type arguments (if any).
     * <p>
     * Example outputs:
     * - "java.lang.String"
     * - "org.acme.Outer.Inner<java.lang.Integer>"
     * - "? extends java.util.List<? super java.lang.Number>"
     * - "int[]"
     * - "T"
     */
    public static String describeResolvedType(ResolvedType type) {
        if (type.isPrimitive()) {
            // E.g. "int", "boolean"
            return type.asPrimitive().describe();
        }
        if (type.isVoid()) {
            // "void"
            return "void";
        }
        if (type.isArray()) {
            // E.g. "java.lang.String" + "[]" => "java.lang.String[]"
            ResolvedArrayType arrayType = type.asArrayType();
            return describeResolvedType(arrayType.getComponentType()) + "[]";
        }
        if (type.isReferenceType()) {
            // Class, interface, enum, or parameterized type: e.g. "java.util.List<java.lang.String>"
            return describeReferenceType(type.asReferenceType());
        }
        if (type.isTypeVariable()) {
            // E.g. "T"
            return type.asTypeVariable().describe();
        }
        if (type.isWildcard()) {
            // E.g. "? extends X" or "? super Y"
            return describeWildcard(type.asWildcard());
        }

        // Fallback for anything unusual
        return type.describe();
    }

    private static String describeReferenceType(ResolvedReferenceType refType) {
        // Get the raw name (e.g., "java.util.List")
        String rawName = refType.getQualifiedName();

        // Then gather type arguments, if any
        if (!refType.typeParametersMap().isEmpty()) {
            ResolvedReferenceTypeDeclaration declaration = refType.getTypeDeclaration().orElseThrow();
            String joined = declaration.getTypeParameters().stream()
                    .map(d -> refType.typeParametersMap().getValueBySignature(refType.getQualifiedName() + "." + d.getName())
                            .orElseThrow())
                    .map(ResolvedTypeDescriber::describeResolvedType) // recursion
                    .collect(Collectors.joining(", "));
            return rawName + "<" + joined + ">";
        }
        return rawName; // no type parameters
    }

    private static String describeWildcard(ResolvedWildcard wildcard) {
        // JavaParser symbol solver can represent "? extends X" or "? super Y" or "?"
        if (!wildcard.isBounded()) {
            return "?"; // unbounded
        }
        ResolvedType bound = wildcard.getBoundedType();
        if (wildcard.isExtends()) {
            // "? extends SomeType"
            return "? extends " + describeResolvedType(bound);
        } else {
            // "? super SomeType"
            return "? super " + describeResolvedType(bound);
        }
    }
}
