package io.smallrye.mutiny.vertx.apigenerator.collection;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import io.smallrye.mutiny.vertx.apigenerator.JavadocHelper;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.vertx.codegen.annotations.Fluent;

public class VertxGenMethod {

    public record ResolvedParameter(String name, ResolvedType type) {
    }

    private final MethodDeclaration method;
    private final String methodName;

    private final ResolvedType returnedType;

    private final List<ResolvedParameter> parameters;
    private final List<ResolvedType> exceptions;
    private final boolean isDeprecated;
    private final boolean isStatic;
    private final boolean isFluent;
    private final Javadoc javadoc;
    private final boolean isFinal;
    private final List<ResolvedTypeParameterDeclaration> typeParameters;

    public VertxGenMethod(MethodDeclaration method) {
        ResolvedMethodDeclaration resolved = method.resolve();
        this.method = method;
        this.methodName = resolved.getName();
        this.returnedType = resolved.getReturnType();
        this.parameters = new ArrayList<>();
        for (int i = 0; i < resolved.getNumberOfParams(); i++) {
            ResolvedParameterDeclaration param = resolved.getParam(i);
            parameters.add(new ResolvedParameter(param.getName(), param.getType()));
        }
        this.exceptions = resolved.getSpecifiedExceptions();
        this.typeParameters = resolved.getTypeParameters();

        this.isDeprecated = method.getAnnotationByClass(Deprecated.class).isPresent();
        this.isStatic = method.isStatic();
        this.isFluent = method.getAnnotationByClass(Fluent.class).isPresent();
        this.javadoc = method.getJavadoc().orElse(null);
        this.isFinal = method.isFinal();
    }

    public VertxGenMethod(MethodDeclaration decl, MethodUsage usage) {
        this.method = decl;
        this.methodName = usage.getName();
        this.returnedType = usage.returnType();
        this.parameters = new ArrayList<>();
        for (int i = 0; i < usage.getParamTypes().size(); i++) {
            ResolvedType type = usage.getParamTypes().get(i);
            String name = usage.getDeclaration().getParam(i).getName();
            parameters.add(new ResolvedParameter(name, type));
        }
        this.isStatic = usage.getDeclaration().isStatic();
        this.exceptions = usage.exceptionTypes();
        this.typeParameters = usage.getDeclaration().getTypeParameters();
        if (decl != null) {
            this.isDeprecated = method.getAnnotationByClass(Deprecated.class).isPresent();
            this.isFluent = method.getAnnotationByClass(Fluent.class).isPresent();
            this.javadoc = method.getJavadoc().orElse(null);
            this.isFinal = method.isFinal();
        } else {
            this.isFinal = false;
            this.isDeprecated = false;
            this.isFluent = false;
            this.javadoc = null;
        }
    }

    public String getName() {
        return methodName;
    }

    public ResolvedType getReturnedType() {
        return returnedType;
    }

    public List<ResolvedType> getThrownExceptions() {
        return exceptions;
    }

    public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
        return typeParameters;
    }

    public boolean isDeprecated() {
        return isDeprecated;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public Javadoc getJavadoc(ShimClass shimClass) {
        return JavadocHelper.toMutinyTypes(javadoc, shimClass);
    }

    public List<ResolvedParameter> getParameters() {
        return parameters;
    }

    public MethodDeclaration getMethod() {
        return method;
    }

    public boolean isFluent() {
        return isFluent;
    }

    public boolean isFinal() {
        return isFinal;
    }
}
