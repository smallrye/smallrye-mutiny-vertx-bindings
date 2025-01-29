package io.smallrye.mutiny.vertx.apigenerator;

import java.util.Optional;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;

import io.vertx.codegen.annotations.GenIgnore;

public class AnnotationHelper {

    public static Optional<MemberValuePair> getAttribute(AnnotationExpr expr, String attribute) {
        if (expr.isMarkerAnnotationExpr()) {
            return Optional.empty();
        }
        if (expr.isSingleMemberAnnotationExpr()) {
            return Optional.of(new MemberValuePair("value", expr.asSingleMemberAnnotationExpr().getMemberValue()));
        }
        return expr.asNormalAnnotationExpr().getPairs().stream()
                .filter(p -> p.getNameAsString().equals(attribute)).findFirst();
    }

    public static boolean isIgnored(MethodDeclaration method) {
        boolean present = method.isAnnotationPresent(GenIgnore.class);
        if (present) {
            AnnotationExpr expr = method.getAnnotationByClass(GenIgnore.class).orElseThrow();
            return isIgnored(expr);
        }
        return false;
    }

    private static boolean isIgnored(AnnotationExpr expr) {
        Optional<MemberValuePair> value = getAttribute(expr, "value");

        if (value.isEmpty()) {
            return true;
        } else {
            if (value.get().getValue().isFieldAccessExpr()) {
                return value.get().getValue().asFieldAccessExpr().getNameAsString().equals(GenIgnore.PERMITTED_TYPE);
            } else if (value.get().getValue().isNameExpr()) {
                return value.get().getValue().asNameExpr().getNameAsString().equals(GenIgnore.PERMITTED_TYPE);
            } else {
                return true;
            }
        }
    }

    public static boolean isIgnored(FieldDeclaration field) {
        boolean present = field.isAnnotationPresent(GenIgnore.class);
        if (present) {
            AnnotationExpr expr = field.getAnnotationByClass(GenIgnore.class).orElseThrow();
            return isIgnored(expr);
        }
        return false;
    }
}
