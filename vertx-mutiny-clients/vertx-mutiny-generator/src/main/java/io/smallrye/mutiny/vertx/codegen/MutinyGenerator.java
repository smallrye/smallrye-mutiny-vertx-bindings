package io.smallrye.mutiny.vertx.codegen;

import io.smallrye.mutiny.vertx.TypeArg;
import io.smallrye.mutiny.vertx.codegen.lang.*;
import io.smallrye.mutiny.vertx.codegen.methods.*;
import io.vertx.codegen.*;
import io.vertx.codegen.annotations.ModuleGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.codegen.type.ClassTypeInfo;
import io.vertx.codegen.type.ParameterizedTypeInfo;
import io.vertx.codegen.type.TypeInfo;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.vertx.codegen.type.ClassKind.API;
import static java.util.stream.Collectors.joining;

public class MutinyGenerator extends Generator<ClassModel> {

    public static final String ID = "mutiny";
    private List<MethodInfo> methods = new ArrayList<>();
    private final Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap = new HashMap<>();

    public static List<String> IGNORED_TYPES = Arrays.asList(
            Future.class.getName(),
            CompositeFuture.class.getName()
    );

    MutinyGenerator() {
        this.kinds = Collections.singleton("class");
        this.name = "mutiny";
    }

    protected void genMethods(ClassModel model, MethodInfo method, List<String> cacheDecls, PrintWriter writer) {
        generateMethod(model, method, cacheDecls, writer);
        MethodInfo publisherOverload = genOverloadedMethod(method);
        if (publisherOverload != null) {
            generateMethod(model, publisherOverload, cacheDecls, writer);
        }
    }

    private MethodInfo genOverloadedMethod(MethodInfo method) {
        List<ParamInfo> params = null;
        int count = 0;
        for (ParamInfo param : method.getParams()) {
            if (param.getType().isParameterized()
                    && param.getType().getRaw().getName().equals("io.vertx.core.streams.ReadStream")) {
                if (params == null) {
                    params = new ArrayList<>(method.getParams());
                }
                ParameterizedTypeInfo paramType = new io.vertx.codegen.type.ParameterizedTypeInfo(
                        io.vertx.codegen.type.TypeReflectionFactory.create(org.reactivestreams.Publisher.class).getRaw(),
                        false,
                        Collections.singletonList(((ParameterizedTypeInfo) param.getType()).getArg(0)));
                params.set(count, new io.vertx.codegen.ParamInfo(
                        param.getIndex(),
                        param.getName(),
                        param.getDescription(),
                        paramType));
            }
            count = count + 1;
        }
        if (params != null) {
            return method.copy().setParams(params);
        }
        return null;
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

            (mode, writer) ->
                    methodTypeArgMap.forEach((method, map) -> map.forEach(
                            (typeArg, identifier) -> genTypeArgDecl(typeArg, method, identifier, writer))),

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
                    methods.forEach(method -> generateMethodDeclaration(model, method, Collections.emptyList(), writer));
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

    final void generateMethod(ClassModel model, MethodInfo method, List<String> cacheDecls, PrintWriter writer) {
        UniMethodGenerator uni = new UniMethodGenerator(writer);
        ForgetMethodGenerator forget = new ForgetMethodGenerator(writer);
        AwaitMethodGenerator await = new AwaitMethodGenerator(writer);
        ConsumerMethodGenerator consumer = new ConsumerMethodGenerator(writer);
        SimpleMethodGenerator simple = new SimpleMethodGenerator(writer, cacheDecls, methodTypeArgMap);
        if (CodeGenHelper.methodKind(method) == MethodKind.FUTURE) {
            simple.generate(model, method);
            uni.generate(method);
            await.generate(method);
            forget.generate(model, method);
        } else if (CodeGenHelper.methodKind(method) == MethodKind.HANDLER) {
            simple.generate(model, method);
            consumer.generate(method);
        } else if (CodeGenHelper.methodKind(method) == MethodKind.OTHER) {
            if (method.getReturnType() != null && method.getReturnType().getRaw() != null && method.getReturnType()
                    .getRaw().getName().equals(Future.class.getName())) {
                env.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "A method returning a 'Future' has been found - missing handler method for '" + method.getName()
                                + "' declared in " + method.getOwnerTypes().stream().map(TypeInfo::getName)
                                .collect(joining()));
                uni.generateOther(method);
                await.generateOther(method);
                forget.generateOther(model, method);
            } else {
                simple.generateOther(model, method);
            }
        }
    }

    private void generateMethodDeclaration(ClassModel model, MethodInfo method, List<String> cacheDecls,
            PrintWriter writer) {
        if (CodeGenHelper.methodKind(method) == MethodKind.FUTURE) {
            new UniMethodGenerator(writer).generateDeclaration(method);
            if (model.getMethods().stream()
                    .noneMatch(mi -> mi.getName().equals(method.getName() + AwaitMethodGenerator.SUFFIX_AND_AWAIT))) {
                new AwaitMethodGenerator(writer).generateDeclaration(method);
            }
            if (model.getMethods().stream()
                    .noneMatch(mi -> mi.getName().equals(method.getName() + ForgetMethodGenerator.SUFFIX_AND_FORGET))) {
                new ForgetMethodGenerator(writer).generateDeclaration(model, method);
            }
        } else if (CodeGenHelper.methodKind(method) == MethodKind.HANDLER) {
            ConsumerMethodGenerator consumer = new ConsumerMethodGenerator(writer);
            consumer.generateDeclaration(method);
        } else {
            SimpleMethodGenerator simple = new SimpleMethodGenerator(writer, cacheDecls, methodTypeArgMap);
            simple.generateDeclaration(method);
        }
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

}
