package io.smallrye.mutiny.vertx.apigenerator.analysis;

import java.util.List;

import javax.lang.model.element.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.palantir.javapoet.*;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenMethod;
import io.smallrye.mutiny.vertx.apigenerator.utils.TypeUtils;

public class BaseShimMethod implements ShimMethod {

    private final Type returnType;
    private final String name;
    private final List<ShimMethodParameter> parameters;
    private final List<Type> throwTypes;
    private final boolean isStatic;
    private final boolean isFinal;
    private final ShimModule module;
    private boolean isOverridden;
    private Javadoc javadoc;

    private final VertxGenMethod originalMethod;
    private final boolean fluent;

    private static final Logger logger = LoggerFactory.getLogger(BaseShimMethod.class);

    public BaseShimMethod(ShimModule module, String name, Type returnType, List<ShimMethodParameter> parameters,
            List<Type> throwsTypes, boolean isStatic, boolean isFinal, Javadoc javadoc, VertxGenMethod originalMethod) {
        this.module = module;
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters == null ? List.of() : parameters;
        this.throwTypes = throwsTypes == null ? List.of() : throwsTypes;
        this.isStatic = isStatic;
        this.isFinal = isFinal;
        this.javadoc = javadoc;
        this.originalMethod = originalMethod;
        if (originalMethod != null) {
            this.fluent = originalMethod.isFluent();
        } else {
            this.fluent = false;
        }
    }

    @Override
    public ShimModule declaredBy() {
        return module;
    }

    public void setOverridden(boolean overridden) {
        isOverridden = overridden;
    }

    public void setJavadoc(Javadoc javadoc) {
        this.javadoc = javadoc;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    @Override
    public List<ShimMethodParameter> getParameters() {
        return parameters;
    }

    @Override
    public List<Type> getThrows() {
        return throwTypes;
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
    public boolean isFluent() {
        return fluent;
    }

    @Override
    public Javadoc getJavadoc() {
        return javadoc;
    }

    @Override
    public VertxGenMethod getOriginalMethod() {
        return originalMethod;
    }

    @Override
    public boolean isOverridden() {
        return isOverridden;
    }

    @Override
    public void generate(ShimClass shim, TypeSpec.Builder builder) {
        logger.warn("Generate method not implemented for {}", this.getClass().getName());
    }

    public MethodSpec.Builder generateDeclaration(ShimClass shim, TypeSpec.Builder builder) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(getName());
        if (isOverridden()) {
            method.addAnnotation(Override.class);
        }
        if (getOriginalMethod() != null && getOriginalMethod().isDeprecated()) {
            method.addAnnotation(Deprecated.class);
        }
        if (getJavadoc() != null) {
            // We need to sanitize text like '$i' for JavaPoet
            method.addJavadoc(getJavadoc().toText().replace("$", "$$"));
        }
        method.addModifiers(Modifier.PUBLIC);
        if (isStatic()) {
            method.addModifiers(Modifier.STATIC);
        } else {
            if (shim.isInterface()) {
                method.addModifiers(Modifier.ABSTRACT);
            }
        }
        if (isFinal()) {
            method.addModifiers(Modifier.FINAL);
        }

        method.returns(Shim.getTypeNameFromType(getReturnType()));

        // Parameters
        for (var parameter : getParameters()) {
            method.addParameter(Shim.getTypeNameFromType(parameter.shimType()), parameter.name());
        }

        // Throws
        for (var exception : getThrows()) {
            method.addException(Shim.getTypeNameFromType(exception));
        }

        // Type Parameters
        if (getOriginalMethod() != null) {
            for (ResolvedTypeParameterDeclaration tp : getOriginalMethod().getTypeParameters()) {
                method.addTypeVariable(TypeVariableName.get(tp.getName()));
            }
        }

        if (getOriginalMethod() != null) {
            if (TypeUtils.isFuture(getOriginalMethod().getReturnedType())) {
                method.addAnnotation(CheckReturnValue.class);
            }
        }

        return method;
    }

    public void addGeneratedBy(CodeBlock.Builder code) {
        code.addStatement("// Code generated by " + declaredBy().getClass().getName());
    }

    public void addGeneratedBy(MethodSpec.Builder method) {
        method.addComment("// Code generated by " + declaredBy().getClass().getName());
    }
}
