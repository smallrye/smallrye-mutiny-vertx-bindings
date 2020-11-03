package io.smallrye.mutiny.vertx.codegen;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.ReadStreamSubscriber;
import io.smallrye.mutiny.vertx.TypeArg;
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
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.vertx.codegen.type.ClassKind.API;
import static io.vertx.codegen.type.ClassKind.PRIMITIVE;
import static java.util.stream.Collectors.joining;

public abstract class AbstractMutinyGenerator extends Generator<ClassModel> {

    public static final String ID = "mutiny";
    private List<MethodInfo> methods = new ArrayList<>();
    private final Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap = new HashMap<>();

    public static List<String> IGNORED_TYPES = Arrays.asList(
            Future.class.getName(),
            CompositeFuture.class.getName()
    );

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
        if (IGNORED_TYPES.contains(model.getFqn())) {
            return null;
        }

        initState(model);
        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        generateClass(model, writer);
        return sw.toString();
    }

    private void initState(ClassModel model) {
        initGenMethods(model);
        initCachedTypeArgs();
    }

    private final List<CodeWriter> generators = Arrays.asList(
            new PackageDeclarationCodeWriter(),
            new ImportDeclarationCodeWriter(),
            new ClassJavadocCodeWriter(),
            new MutinyGenAnnotationCodeWriter(),
            new ClassDeclarationCodeWriter(),

            // Class body start here
            new TypeArgsConstantCodeWriter(),
            new DelegateFieldCodeWriter(),

            new ConstructorWithDelegateParameterCodeWriter(),
            new ConstructorWithObjectDelegateCodeWriter(),
            new ConstructorWithGenericTypesCodeWriter(),
            new NoArgConstructorCodeWriter(),
            new GetDelegateMethodCodeWriter(),

            (mode, writer) -> {
                methodTypeArgMap.forEach((method, map) -> {
                    map.forEach((typeArg, identifier) -> {
                        genTypeArgDecl(typeArg, method, identifier, writer);
                    });
                });
            },

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
                    methods.forEach(method -> genMethodDecl(model, method, Collections.emptyList(), writer));
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
        methods.forEach(method -> genMethods(model, method, cacheDecls, writer));


        new ConstantCodeWriter(methodTypeArgMap).apply(model, writer);

        for (String cacheDecl : cacheDecls) {
            writer.print("  ");
            writer.print(cacheDecl);
            writer.println(";");
        }
    }

    /**
     * Compute the list of methods.
     *
     * @param model the class model
     */
    private void initGenMethods(ClassModel model) {
        List<List<MethodInfo>> list = new ArrayList<>();


        List<MethodInfo> infos = model.getMethods().stream()
                // Remove method returning Future as it conflicts with method returning Uni
                .filter(mi -> !mi.getReturnType().getName().equals(Future.class.getName()))
                // Remove methods coming from ignored type
                .filter(mi -> {
                    for (ClassTypeInfo ownerType : mi.getOwnerTypes()) {
                        if (IGNORED_TYPES.contains(ownerType.getName())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());



        list.add(infos);
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
        methods = list.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    /**
     * Build the map of type arguments that can be statically cached within the generated class
     */
    private void initCachedTypeArgs() {
        methodTypeArgMap.clear();
        int count = 0;
        for (MethodInfo method : methods) {
            TypeInfo returnType = method.getReturnType();
            if (returnType instanceof ParameterizedTypeInfo) {
                ParameterizedTypeInfo parameterizedType = (ParameterizedTypeInfo) returnType;
                List<TypeInfo> typeArgs = parameterizedType.getArgs();
                Map<TypeInfo, String> typeArgMap = new HashMap<>();
                for (TypeInfo typeArg : typeArgs) {
                    if (typeArg.getKind() == API && !containsTypeVariableArgument(typeArg)) {
                        String typeArgRef = "TYPE_ARG_" + count++;
                        typeArgMap.put(typeArg, typeArgRef);
                    }
                }
                methodTypeArgMap.put(method, typeArgMap);
            }
        }
    }

    /**
     * @return whether a type contains a nested type variable declaration
     */
    private boolean containsTypeVariableArgument(TypeInfo type) {
        if (type.isVariable()) {
            return true;
        } else if (type.isParameterized()) {
            List<TypeInfo> typeArgs = ((ParameterizedTypeInfo) type).getArgs();
            for (TypeInfo typeArg : typeArgs) {
                if (typeArg.isVariable()) {
                    return true;
                } else if (typeArg.isParameterized() && containsTypeVariableArgument(typeArg)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected abstract void genMethods(ClassModel model, MethodInfo method, List<String> cacheDecls,
            PrintWriter writer);

    protected abstract void genForgetMethod(ClassModel model, MethodInfo method, List<String> cacheDecls,
            PrintWriter writer);

    protected abstract void genUniMethod(boolean decl, ClassModel model, MethodInfo method, PrintWriter writer);

    protected abstract void genUniMethodForOther(boolean decl, ClassModel model, MethodInfo method, PrintWriter writer);


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
            if (model.getMethods().stream().noneMatch(mi -> mi.getName().equals(method.getName() + "AndAwait"))) {
                genBlockingMethod(false, model, method, writer);
            }
            if (model.getMethods().stream().noneMatch(mi -> mi.getName().equals(method.getName() + "AndForget"))) {
                genForgetMethod(model, method, cacheDecls, writer);
            }
        } else if (CodeGenHelper.methodKind(method) == MethodKind.HANDLER) {
            genSimpleMethod(false, model, true, "__" + method.getName(), method, cacheDecls, writer);
            genConsumerMethodInfo(false, model, method, writer);
        } else if (CodeGenHelper.methodKind(method) == MethodKind.OTHER) {
            if (method.getReturnType() != null  && method.getReturnType().getRaw() != null  && method.getReturnType().getRaw().getName().equals(Future.class.getName())) {
                genUniMethodForOther(false, model, method, writer);
            } else {
                genSimpleMethod(false, model, false, method.getName(), method, cacheDecls, writer);
            }
        }
    }

    final void genForgetMethod(boolean decl, ClassModel model, MethodInfo method, List<String> cacheDecls,
            PrintWriter writer) {
        startMethodTemplate(false, method.getName() + "AndForget", method,
                new MethodDescriptor("", true, false, false), writer);
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
                cachedType = CodeGenHelper.genTranslatedTypeName(returnType);
            }
            writer.print("    ");
            writer.print(CodeGenHelper.genTranslatedTypeName(returnType));
            writer.print(" ret = ");
            writer.print(CodeGenHelper
                    .genConvReturn(methodTypeArgMap, returnType, method, genInvokeDelegate(model, method)));
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

    protected static class MethodDescriptor {
        final String deprecated;
        final boolean andForget;
        final boolean andAwait;
        final boolean uni;

        MethodDescriptor(String deprecated, boolean andForget, boolean andAwait, boolean uni) {
            this.deprecated = deprecated;
            this.andForget = andForget;
            this.andAwait = andAwait;
            this.uni = uni;
        }
    }

    void startMethodTemplate(boolean isPrivate, String methodName, MethodInfo method, MethodDescriptor descriptor,
            PrintWriter writer) {
        Doc doc = method.getDoc();
        String deprecated = descriptor.deprecated;
        String link = getJavadocLink(method.getOwnerTypes().iterator().next(), method);
        if (doc != null) {
            writer.println("  /**");
            if (descriptor.uni) {
                Token.toHtml(doc.getTokens(), "   *", CodeGenHelper::renderLinkToHtml, "\n", writer);
                writer.println("   * <p>");
                writer.println("   * Unlike the <em>bare</em> Vert.x variant, this method returns a {@link " + Uni.class
                        .getName() + " Uni}.");
                writer.println("   * Don't forget to <em>subscribe</em> on it to trigger the operation.");
            }

            if (descriptor.andAwait) {
                writer.println("   * Blocking variant of " + link + ".");
                writer.println("   * <p>");
                writer.println("   * This method waits for the completion of the underlying asynchronous operation.");
                writer.println(
                        "   * If the operation completes successfully, the result is returned, otherwise the failure is thrown (potentially wrapped in a RuntimeException).");
            }

            if (descriptor.andForget) {
                writer.println("   * Variant of " + link + " that ignores the result of the operation.");
                writer.println("   * <p>");
                writer.println("   * This method subscribes on the result of " + link
                        + ", but discards the outcome (item or failure).");
                writer.println("   * This method is useful to trigger the asynchronous operation from " + link
                        + " but you don't need to compose it with other operations.");
            }

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
                } else if (descriptor.uni) {
                    writer.print("the {@link " + Uni.class.getName()
                            + " uni} firing the result of the operation when completed, or a failure if the operation failed.");
                } else if (descriptor.andAwait) {
                    writer.print(
                            "the " + method.getReturnType().getSimpleName() + " instance produced by the operation");
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
            writer.println("  @Deprecated");
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
        if (descriptor.andForget) {
            writer.print("void");
        } else {
            writer.print(CodeGenHelper.genTranslatedTypeName(method.getReturnType()));
        }
        writer.print(" ");
        writer.print(methodName);
        writer.print("(");
        writer.print(method.getParams().stream()
                .map(it -> CodeGenHelper.genTranslatedTypeName(it.getType()) + " " + it.getName())
                .collect(joining(", ")));
        writer.print(")");

    }

    private String getJavadocLink(ClassTypeInfo owner, MethodInfo method) {
        return CodeGenHelper.renderLinkToHtml(owner, method);
    }

    private void genSimpleMethod(boolean decl,
            ClassModel model,
            boolean isPrivate,
            String methodName,
            MethodInfo method,
            List<String> cacheDecls,
            PrintWriter writer) {
        startMethodTemplate(isPrivate, methodName, method, new MethodDescriptor(
                "", false, false, false
        ), writer);
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
                cachedType = CodeGenHelper.genTranslatedTypeName(returnType);
            }
            writer.print("    ");
            writer.print(CodeGenHelper.genTranslatedTypeName(returnType));
            writer.print(" ret = ");
            writer.print(CodeGenHelper
                    .genConvReturn(methodTypeArgMap, returnType, method, genInvokeDelegate(model, method)));
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

    public String genInvokeDelegate(ClassModel model, MethodInfo method) {
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
                ret.append(CodeGenHelper.genConvParam(methodTypeArgMap, type, method, param.getName()));
            }
            index = index + 1;
        }
        ret.append(")");
        return ret.toString();
    }

    private void genTypeArgDecl(TypeInfo typeArg, MethodInfo method, String typeArgRef, PrintWriter writer) {
        StringBuilder sb = new StringBuilder();
        CodeGenHelper.genTypeArg(typeArg, method, 1, sb);
        writer.print("  static final ");
        writer.print(TypeArg.class.getName());
        writer.print("<");
        writer.print(typeArg.translateName(ID));
        writer.print("> ");
        writer.print(typeArgRef);
        writer.print(" = ");
        writer.print(sb);
        writer.println(";");
    }

    private static TypeInfo unwrap(TypeInfo type) {
        if (type instanceof ParameterizedTypeInfo) {
            return type.getRaw();
        } else {
            return type;
        }
    }

    private boolean foo(MethodInfo m1, MethodInfo m2) {
        int numParams = m1.getParams().size();
        if (m1.getName().equals(m2.getName()) && numParams == m2.getParams().size()) {
            for (int index = 0; index < numParams; index++) {
                TypeInfo t1 = unwrap(m1.getParam(index).getType());
                TypeInfo t2 = unwrap(m2.getParam(index).getType());
                if (!t1.equals(t2)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
