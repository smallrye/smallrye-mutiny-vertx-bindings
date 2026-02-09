package io.smallrye.mutiny.vertx.apigenerator.types;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import com.palantir.javapoet.*;

public record JavaType(String fqn, List<JavaType> parameterTypes) {

    JavaType(String fqn) {
        this(fqn, new ArrayList<>());
    }

    public String describe() {
        if (parameterTypes.isEmpty()) {
            return fqn;
        } else {
            String args = parameterTypes.stream()
                    .map(JavaType::describe)
                    .collect(Collectors.joining(","));
            return fqn + "<" + args + ">";
        }
    }

    public boolean hasParameterTypes() {
        return !parameterTypes.isEmpty();
    }

    public TypeName toTypeName() {
        Type asPrimitive = asPrimitive(fqn);
        if (asPrimitive != null) {
            return TypeName.get(asPrimitive);
        }
        if (fqn.equals("void")) {
            return TypeName.VOID;
        }
        if (fqn.endsWith("[]")) {
            TypeName arrayTypeName = new JavaType(fqn.substring(0, fqn.length() - 2)).toTypeName();
            return ArrayTypeName.of(arrayTypeName);
        }
        if (!hasParameterTypes()) {
            return ClassName.bestGuess(fqn);
        }
        TypeName[] typeNames = new TypeName[parameterTypes.size()];
        for (int i = 0; i < parameterTypes.size(); i++) {
            JavaType type = parameterTypes.get(i);
            if (type.fqn().startsWith("? extends ")) {
                TypeName extendedTypeName = new JavaType(type.fqn().substring("? extends ".length())).toTypeName();
                typeNames[i] = WildcardTypeName.subtypeOf(extendedTypeName);
            } else if (type.fqn().startsWith("? super ")) {
                TypeName superTypeName = new JavaType(type.fqn().substring("? super ".length())).toTypeName();
                typeNames[i] = WildcardTypeName.supertypeOf(superTypeName);
            } else if (type.hasParameterTypes()) {
                typeNames[i] = type.toTypeName();
            } else {
                typeNames[i] = ClassName.bestGuess(type.fqn().trim());
            }
        }
        return ParameterizedTypeName.get(ClassName.bestGuess(fqn.trim()), typeNames);
    }

    private Type asPrimitive(String fqn) {
        return switch (fqn) {
            case "int" -> int.class;
            case "long" -> long.class;
            case "double" -> double.class;
            case "float" -> float.class;
            case "short" -> short.class;
            case "byte" -> byte.class;
            case "char" -> char.class;
            case "boolean" -> boolean.class;
            default -> null;
        };
    }

    public static JavaType of(String representation) {
        String repr = representation
                .replaceAll("\\h", "")
                .replaceAll("\\?extends", "? extends ")
                .replaceAll("\\?super", "? super ");
        if (repr.contains("<")) {
            if (!repr.endsWith(">")) {
                throw new IllegalArgumentException("Invalid Java shimType representation: " + repr);
            }
            Stack<JavaType> stack = new Stack<>();
            StringBuilder buffer = new StringBuilder();
            JavaType rootType = null;
            for (int i = 0; i < repr.length(); i++) {
                char next = repr.charAt(i);
                switch (next) {
                    case '<' -> {
                        JavaType type = new JavaType(buffer.toString().trim());
                        if (!stack.isEmpty()) {
                            stack.peek().parameterTypes().add(type);
                        }
                        stack.push(type);
                        buffer.setLength(0);
                        if (rootType == null) {
                            rootType = type;
                        }
                    }
                    case '>' -> {
                        if (!buffer.isEmpty()) {
                            JavaType type = new JavaType(buffer.toString().trim());
                            stack.peek().parameterTypes().add(type);
                            buffer.setLength(0);
                        }
                        stack.pop();
                    }
                    case ',' -> {
                        if (!buffer.isEmpty()) {
                            JavaType type = new JavaType(buffer.toString().trim());
                            stack.peek().parameterTypes().add(type);
                            buffer.setLength(0);
                        }
                    }
                    default -> buffer.append(next);
                }
            }
            return rootType;
        } else {
            return new JavaType(repr);
        }
    }
}
