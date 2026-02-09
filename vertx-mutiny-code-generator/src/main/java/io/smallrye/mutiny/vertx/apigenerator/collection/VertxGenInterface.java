package io.smallrye.mutiny.vertx.apigenerator.collection;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.javadoc.Javadoc;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A structure to represent an interface annotated with {@link io.vertx.codegen.annotations.VertxGen}.
 */
public class VertxGenInterface {

    private final ClassOrInterfaceDeclaration declaration;

    private final String fullyQualifiedName;

    private final Javadoc javadoc;

    private final String packageName;

    private final boolean concrete;
    private final boolean deprecated;

    private final List<VertxGenMethod> methods;
    private final List<VertxGenConstant> constants;

    private final VertxGenModule module;
    private final MutinyGenerator generator;

    public VertxGenInterface(CompilationUnit unit,
            ClassOrInterfaceDeclaration declaration,
            VertxGenModule module,
            String fullyQualifiedName,
            boolean concrete,
            List<VertxGenMethod> methods,
            List<VertxGenConstant> constants,
            MutinyGenerator generator) {
        this.module = module;
        this.declaration = declaration;
        this.fullyQualifiedName = fullyQualifiedName;
        this.packageName = unit.getPackageDeclaration().map(p -> p.getName().asString()).orElse("");
        this.javadoc = declaration.getJavadoc().orElse(null);
        this.concrete = concrete;
        this.generator = generator;
        this.methods = methods;
        this.constants = constants;
        this.deprecated = declaration.getAnnotationByClass(Deprecated.class).isPresent();
    }

    public VertxGenModule getModule() {
        return module;
    }

    public boolean isConcrete() {
        return concrete;
    }

    public ClassOrInterfaceDeclaration getDeclaration() {
        return declaration;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public Javadoc getJavadoc() {
        return javadoc;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<VertxGenConstant> getConstants() {
        return constants;
    }

    public List<VertxGenMethod> getMethods() {
        return methods;
    }

    public VertxGenMethod getMethod(String name) {
        return methods.stream().filter(m -> m.getName().equals(name)).findFirst().orElse(null);
    }

    public MutinyGenerator getGenerator() {
        return generator;
    }

    public Type getType() {
        ClassOrInterfaceType type = StaticJavaParser.parseClassOrInterfaceType(
                getDeclaration().getFullyQualifiedName().orElseThrow());
        if (!getDeclaration().getTypeParameters().isEmpty()) {
            Type[] types = new Type[getDeclaration().getTypeParameters().size()];
            for (int i = 0; i < getDeclaration().getTypeParameters().size(); i++) {
                TypeParameter tp = getDeclaration().getTypeParameters().get(i);
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

    public Collection<TypeParameter> getTypeParameters() {
        return new ArrayList<>(declaration.getTypeParameters());
    }

    public String getSimpleName() {
        return declaration.getNameAsString();
    }

    public boolean hasMethod(String name, List<String> parameterTypes) {
        for (VertxGenMethod method : methods) {
            if (method.getName().equals(name)) {
                if (method.getParameters().size() == parameterTypes.size()) {
                    boolean match = true;
                    for (int i = 0; i < parameterTypes.size(); i++) {
                        VertxGenMethod.ResolvedParameter p = method.getParameters().get(i);
                        if (!p.type().describe().equals(parameterTypes.get(i))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isDeprecated() {
        return deprecated;
    }
}
