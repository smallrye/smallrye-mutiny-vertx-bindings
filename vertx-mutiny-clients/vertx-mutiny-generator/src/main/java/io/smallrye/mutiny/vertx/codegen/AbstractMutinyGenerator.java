package io.smallrye.mutiny.vertx.codegen;

import io.smallrye.mutiny.vertx.ReadStreamSubscriber;
import io.smallrye.mutiny.vertx.codegen.lang.*;
import io.vertx.codegen.*;
import io.vertx.codegen.annotations.ModuleGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.codegen.doc.Doc;
import io.vertx.codegen.doc.Token;
import io.vertx.codegen.type.ClassTypeInfo;
import io.vertx.codegen.type.ParameterizedTypeInfo;
import io.vertx.codegen.type.PrimitiveTypeInfo;
import io.vertx.codegen.type.TypeInfo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.vertx.codegen.type.ClassKind.PRIMITIVE;
import static java.util.stream.Collectors.joining;

public abstract class AbstractMutinyGenerator extends Generator<ClassModel> {

    public static final String ID = "mutiny";
    private List<MethodInfo> forget = new ArrayList<>();

    public AbstractMutinyGenerator() {
        this.kinds = Collections.singleton("class");
    }

    @Override
    public Collection<Class<? extends Annotation>> annotations() {
        return Arrays.asList(VertxGen.class, ModuleGen.class);
    }

    @Override
    public String filename(ClassModel model) {
        ModuleInfo module = model.getModule();
        return module.translateQualifiedName(model.getFqn(), ID) + ".java";
    }

    @Override
    public String render(ClassModel model, int index, int size, Map<String, Object> session) {
        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        generateClass(model, writer);
        return sw.toString();
    }

    private List<CodeWriter> generators = Arrays.asList(
            new PackageDeclarationCodeWriter(),
            new ImportDeclarationCodeWriter(),
            new ClassJavadocCodeWriter(),
            new MutinyGenAnnotationCodeWriter(),
            new ClassDeclarationCodeWriter(),

            // Class body start here
            new TypeArgsConstantCodeWriter(),
            new DelegateFieldCodeWriter(),

            new ConstructorWithDelegateParameterCodeWriter(),
            new ConstructorWithGenericTypesCodeWriter(),
            new NoArgConstructorCodeWriter(),
            new GetDelegateMethodCodeWriter(),

            new DelegateMethodDeclarationCodeWriter(),
            new BufferRelatedMethodCodeWriter(),
            new ToStringMethodCodeWriter(),
            new HashCodeAndEqualsMethodsCodeWriter(),
            new IteratableMethodCodeWriter(),
            new IteratorMethodsCodeWriter(),
            new FunctionApplyMethodCodeWriter(),
            new ToSubscriberCodeWriter(),
            new ReadStreamMethodDeclarationCodeWriter(),

            (model, writer) -> {
                if (model.isConcrete()) {
                    generateClassBody(model, writer);
                } else {
                    getGenMethods(model)
                            .forEach(method -> genMethodDecl(model, method, Collections.emptyList(), writer));
                }
            },

            new ToMultiMethodCodeWriter(),
            new NewInstanceMethodCodeWriter(),
            new NewInstanceWithGenericsMethodCodeWriter(),

            (model, writer) -> writer.println("}"), // end of class
            new ImplClassCodeWriter(this)
    );

    private void generateClass(ClassModel model, PrintWriter writer) {
        generators.forEach(cw -> cw.apply(model, writer));
    }

    public void generateClassBody(ClassModel model, PrintWriter writer) {
        List<String> cacheDecls = new ArrayList<>();

        // This list filters out method that conflict during the generation
        Stream<MethodInfo> list = getGenMethods(model);
        list.forEach(method -> genMethods(model, method, cacheDecls, writer));
        // Generate AndForget method
        forget.forEach(method -> genForgetMethods(model, method, cacheDecls, writer));

        new ConstantCodeWriter().apply(model, writer);

        for (String cacheDecl : cacheDecls) {
            writer.print("  ");
            writer.print(cacheDecl);
            writer.println(";");
        }
    }

    /**
     * Build the list of methods to generate.
     *
     * @param model the class model
     * @return the list of methods as a stream
     */
    private Stream<MethodInfo> getGenMethods(ClassModel model) {
        forget = new ArrayList<>();
        List<List<MethodInfo>> list = new ArrayList<>();
        list.add(model.getMethods());
        list.add(model.getAnyJavaTypeMethods());
        list.forEach(methods -> {

            // First pass: filter conflicting overrides, that will partly filter it
            ListIterator<MethodInfo> it = methods.listIterator();
            while (it.hasNext()) {
                MethodInfo method = it.next();
                if (CodeGenHelper.methodKind(method) != MethodKind.FUTURE) {
                    // Has it been removed above ?
                    Predicate<MethodInfo> pred;
                    if (method.isOwnedBy(model.getType())) {
                        pred = other -> isOverride(method, other);
                    } else {
                        pred = other -> isOverride(method, other);
                    }
                    if (methods.stream()
                            .filter(m -> CodeGenHelper.methodKind(m) == MethodKind.FUTURE)
                            .anyMatch(pred)) {
                        // These methods are removed because it generated a signature conflict.
                        // We store them in a specific list and generate "andForget" methods
                        forget.add(method);
                        it.remove();
                    }
                }
            }

            // Second pass: filter future methods that might be still conflict
            it = methods.listIterator();
            while (it.hasNext()) {
                MethodInfo meth = it.next();
                if (CodeGenHelper.methodKind(meth) == MethodKind.FUTURE) {
                    boolean remove;
                    List<MethodInfo> abc = model.getMethodMap().getOrDefault(meth.getName(), Collections.emptyList());
                    if (meth.isOwnedBy(model.getType())) {
                        remove = abc.stream()
                                .filter(m -> CodeGenHelper.methodKind(m) != MethodKind.FUTURE && isOverride(m, meth))
                                .anyMatch(m -> !m.isOwnedBy(model.getType()) || methods.contains(m));
                    } else {
                        remove = abc.stream()
                                .filter(other -> CodeGenHelper.methodKind(other) != MethodKind.FUTURE)
                                .anyMatch(other -> {
                                    if (CodeGenHelper.methodKind(other) != MethodKind.FUTURE) {
                                        Set<ClassTypeInfo> tmp = new HashSet<>(other.getOwnerTypes());
                                        tmp.retainAll(meth.getOwnerTypes());
                                        return isOverride(meth, other) & !tmp.isEmpty();
                                    }
                                    return false;
                                });
                    }
                    if (remove) {
                        it.remove();
                    }
                }
            }
        });
        return list.stream().flatMap(Collection::stream);
    }

    protected abstract void genMethods(ClassModel model, MethodInfo method, List<String> cacheDecls,
            PrintWriter writer);

    protected abstract void genForgetMethods(ClassModel model, MethodInfo method, List<String> cacheDecls,
            PrintWriter writer);

    protected abstract void genUniMethod(boolean decl, ClassModel model, MethodInfo method, PrintWriter writer);
    protected abstract void genBlockingMethod(boolean decl, ClassModel model, MethodInfo method, PrintWriter writer);

    protected abstract MethodInfo genConsumerMethodInfo(MethodInfo method);

    protected abstract void genConsumerMethodInfo(boolean decl, ClassModel model, MethodInfo method,
            PrintWriter writer);

    private boolean isOverride(MethodInfo s1, MethodInfo s2) {
        if (s1.getName().equals(s2.getName()) && s1.getParams().size() == s2.getParams().size() - 1) {
            for (int i = 0; i < s1.getParams().size(); i++) {
                if (!s1.getParams().get(i).getType().equals(s2.getParams().get(i).getType())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    final void genMethod(ClassModel model, MethodInfo method, List<String> cacheDecls, PrintWriter writer) {
        if (CodeGenHelper.methodKind(method) == MethodKind.FUTURE) {
            genSimpleMethod(false, model, true, "__" + method.getName(), method, cacheDecls, writer);
            genUniMethod(false, model, method, writer);
            if (!model.getMethods().stream().anyMatch(mi -> mi.getName().equals(method.getName() + "AndAwait"))) {
                genBlockingMethod(false, model, method, writer);
            }
        } else if (CodeGenHelper.methodKind(method) == MethodKind.HANDLER) {
            genSimpleMethod(false, model, true, "__" + method.getName(), method, cacheDecls, writer);
            genConsumerMethodInfo(false, model, method, writer);
        } else {
            genSimpleMethod(false, model, false, method.getName(), method, cacheDecls, writer);
        }
    }

    final void genForgetMethod(boolean decl, ClassModel model, MethodInfo method, List<String> cacheDecls, PrintWriter writer) {
        startMethodTemplate(false, method.getName()+ "AndForget", method, "", writer);
        if (decl) {
            writer.println(";");
            return;
        }

        writer.println(" { ");
        if (method.isFluent()) {
            writer.print("    ");
            writer.print(genInvokeDelegate(model, method));
            writer.println(";");
            if (method.getReturnType().isVariable()) {
                writer.print("    return (");
                writer.print(method.getReturnType().getName());
                writer.println(") this;");
            } else {
                writer.println("    return this;");
            }
        } else if (method.getReturnType().getName().equals("void")) {
            writer.print("    ");
            writer.print(genInvokeDelegate(model, method));
            writer.println(";");
        } else {
            if (method.isCacheReturn()) {
                writer.print("    if (cached_");
                writer.print(cacheDecls.size());
                writer.println(" != null) {");

                writer.print("      return cached_");
                writer.print(cacheDecls.size());
                writer.println(";");
                writer.println("    }");
            }
            String cachedType;
            TypeInfo returnType = method.getReturnType();
            if (method.getReturnType().getKind() == PRIMITIVE) {
                cachedType = ((PrimitiveTypeInfo) returnType).getBoxed().getName();
            } else {
                cachedType = CodeGenHelper.genTypeName(returnType);
            }
            writer.print("    ");
            writer.print(CodeGenHelper.genTypeName(returnType));
            writer.print(" ret = ");
            writer.print(CodeGenHelper.genConvReturn(returnType, method, genInvokeDelegate(model, method)));
            writer.println(";");
            if (method.isCacheReturn()) {
                writer.print("    cached_");
                writer.print(cacheDecls.size());
                writer.println(" = ret;");
                cacheDecls.add("private" + (method.isStaticMethod() ? " static" : "") + " " + cachedType + " cached_"
                        + cacheDecls.size());
            }
            writer.println("    return ret;");
        }
        writer.println("  }");
        writer.println();
    }

    private void genMethodDecl(ClassModel model, MethodInfo method, List<String> cacheDecls,
            PrintWriter writer) {
        if (CodeGenHelper.methodKind(method) == MethodKind.FUTURE) {
            genUniMethod(true, model, method, writer);
            if (!model.getMethods().stream().anyMatch(mi -> mi.getName().equals(method.getName() + "AndAwait"))) {
                genBlockingMethod(true, model, method, writer);
            }
        } else if (CodeGenHelper.methodKind(method) == MethodKind.HANDLER) {
            genConsumerMethodInfo(true, model, method, writer);
        } else {
            genSimpleMethod(true, model, false, method.getName(), method, cacheDecls, writer);
        }
    }

    void startMethodTemplate(boolean isPrivate, String methodName, MethodInfo method, String deprecated,
            PrintWriter writer) {
        Doc doc = method.getDoc();
        if (doc != null) {
            writer.println("  /**");
            Token.toHtml(doc.getTokens(), "   *", CodeGenHelper::renderLinkToHtml, "\n", writer);
            for (ParamInfo param : method.getParams()) {
                writer.print("   * @param ");
                writer.print(param.getName());
                writer.print(" ");
                if (param.getDescription() != null) {
                    Token.toHtml(param.getDescription().getTokens(), "", CodeGenHelper::renderLinkToHtml, "",
                            writer);
                }
                writer.println();
            }
            if (!method.getReturnType().getName().equals("void")) {
                writer.print("   * @return ");
                if (method.getReturnDescription() != null) {
                    Token.toHtml(method.getReturnDescription().getTokens(), "",
                            CodeGenHelper::renderLinkToHtml, "",
                            writer);
                }
                writer.println();
            }
            if (deprecated != null && deprecated.length() > 0) {
                writer.print("   * @deprecated ");
                writer.println(deprecated);
            }
            writer.println("   */");
        }
        if (method.isDeprecated() || deprecated != null && deprecated.length() > 0) {
            writer.println("  @Deprecated()");
        }
        writer.print("  " + (isPrivate ? "private" : "public") + " ");
        if (method.isStaticMethod()) {
            writer.print("static ");
        }
        if (method.getTypeParams().size() > 0) {
            writer.print(
                    method.getTypeParams().stream().map(TypeParamInfo::getName).collect(joining(", ", "<", ">")));
            writer.print(" ");
        }
        writer.print(CodeGenHelper.genTypeName(method.getReturnType()));
        writer.print(" ");
        writer.print(methodName);
        writer.print("(");
        writer.print(method.getParams().stream().map(it -> CodeGenHelper.genTypeName(it.getType()) + " " + it.getName())
                .collect(joining(", ")));
        writer.print(")");

    }

    private void genSimpleMethod(boolean decl,
            ClassModel model,
            boolean isPrivate,
            String methodName,
            MethodInfo method,
            List<String> cacheDecls,
            PrintWriter writer) {
        startMethodTemplate(isPrivate, methodName, method, "", writer);
        if (decl) {
            writer.println(";");
            return;
        }
        writer.println(" { ");
        if (method.isFluent()) {
            writer.print("    ");
            writer.print(genInvokeDelegate(model, method));
            writer.println(";");
            if (method.getReturnType().isVariable()) {
                writer.print("    return (");
                writer.print(method.getReturnType().getName());
                writer.println(") this;");
            } else {
                writer.println("    return this;");
            }
        } else if (method.getReturnType().getName().equals("void")) {
            writer.print("    ");
            writer.print(genInvokeDelegate(model, method));
            writer.println(";");
        } else {
            if (method.isCacheReturn()) {
                writer.print("    if (cached_");
                writer.print(cacheDecls.size());
                writer.println(" != null) {");

                writer.print("      return cached_");
                writer.print(cacheDecls.size());
                writer.println(";");
                writer.println("    }");
            }
            String cachedType;
            TypeInfo returnType = method.getReturnType();
            if (method.getReturnType().getKind() == PRIMITIVE) {
                cachedType = ((PrimitiveTypeInfo) returnType).getBoxed().getName();
            } else {
                cachedType = CodeGenHelper.genTypeName(returnType);
            }
            writer.print("    ");
            writer.print(CodeGenHelper.genTypeName(returnType));
            writer.print(" ret = ");
            writer.print(CodeGenHelper.genConvReturn(returnType, method, genInvokeDelegate(model, method)));
            writer.println(";");
            if (method.isCacheReturn()) {
                writer.print("    cached_");
                writer.print(cacheDecls.size());
                writer.println(" = ret;");
                cacheDecls.add("private" + (method.isStaticMethod() ? " static" : "") + " " + cachedType + " cached_"
                        + cacheDecls.size());
            }
            writer.println("    return ret;");
        }
        writer.println("  }");
        writer.println();
    }

    private String genInvokeDelegate(ClassModel model, MethodInfo method) {
        StringBuilder ret;
        if (method.isStaticMethod()) {
            ret = new StringBuilder(Helper.getNonGenericType(model.getIfaceFQCN()));
        } else {
            ret = new StringBuilder("delegate");
        }
        ret.append(".").append(method.getName()).append("(");
        int index = 0;
        for (ParamInfo param : method.getParams()) {
            if (index > 0) {
                ret.append(", ");
            }
            TypeInfo type = param.getType();
            if (type.isParameterized() && (type.getRaw().getName().equals("org.reactivestreams.Publisher"))) {
                String adapterFunction;
                ParameterizedTypeInfo parameterizedType = (ParameterizedTypeInfo) type;
                if (parameterizedType.getArg(0).isVariable()) {
                    adapterFunction = "java.util.function.Function.identity()";
                } else {
                    adapterFunction =
                            "obj -> (" + parameterizedType.getArg(0).getRaw().getName() + ")obj.getDelegate()";
                }
                ret.append(ReadStreamSubscriber.class.getName()).append(".asReadStream(")
                        .append(param.getName())
                        .append(",")
                        .append(adapterFunction).append(").resume()");
            } else {
                ret.append(CodeGenHelper.genConvParam(type, method, param.getName()));
            }
            index = index + 1;
        }
        ret.append(")");
        return ret.toString();
    }

}
