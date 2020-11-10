package io.smallrye.mutiny.vertx.codegen;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.AsyncResultUni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.smallrye.mutiny.vertx.codegen.lang.CodeGenHelper;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.MethodInfo;
import io.vertx.codegen.ParamInfo;
import io.vertx.codegen.type.ClassTypeInfo;
import io.vertx.codegen.type.ParameterizedTypeInfo;
import io.vertx.codegen.type.TypeInfo;
import io.vertx.codegen.type.VoidTypeInfo;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.vertx.codegen.type.ClassKind.API;

public class MutinyGenerator extends AbstractMutinyGenerator {

    MutinyGenerator() {
        this.kinds = Collections.singleton("class");
        this.name = "mutiny";
    }

    @Override
    protected void genMethods(ClassModel model, MethodInfo method, List<String> cacheDecls, PrintWriter writer) {
        genMethod(model, method, cacheDecls, writer);
        MethodInfo publisherOverload = genOverloadedMethod(method, org.reactivestreams.Publisher.class);
        if (publisherOverload != null) {
            genMethod(model, publisherOverload, cacheDecls, writer);
        }
    }

    @Override
    protected void genAndForgetMethod(ClassModel model, MethodInfo method, List<String> cacheDecls,
            PrintWriter writer) {
        MethodInfo forgetMethod = genForgetMethodInfo(method);
        Optional<TypeInfo> fluentType = getFluentType(model, method.getName());
        boolean fluent = false;
        if (fluentType.isPresent()) {
            fluent = true;
            forgetMethod.setReturnType(fluentType.get());
        }

        startMethodTemplate(false, forgetMethod.getName() + "AndForget", forgetMethod,
                new MethodDescriptor("", true, false, false, fluent),
                writer);

        writer.println(" { ");
        writer.print("  " + forgetMethod.getName());
        writer.print("(");
        List<ParamInfo> params = forgetMethod.getParams();
        writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
        writer.println(").subscribe().with(x -> {});");
        if (fluent) {
            writer.println("  return this;");
        }
        writer.println("  }");
        writer.println();
    }

    @Override
    protected void genConsumerMethodInfo(boolean decl, ClassModel model, MethodInfo method, PrintWriter writer) {
        MethodInfo futMethod = genConsumerMethodInfo(method);
        startMethodTemplate(false, futMethod.getName(), futMethod,
                new MethodDescriptor("", false, false, false, false),
                writer);
        if (decl) {
            writer.println(";");
            return;
        }
        writer.println(" {");
        writer.print("    ");
        if (!method.getReturnType().isVoid()) {
            writer.print("return ");
        }
        writer.print("__" + method.getName() + "(");
        List<ParamInfo> params = futMethod.getParams();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                writer.print(", ");
            }
            ParamInfo param = params.get(i);
            if (i < params.size() - 1) {
                writer.print(param.getName());
            } else {
                writer.print(param.getName() + " != null ? " + param.getName() + "::accept : null");
            }
        }
        writer.println(");");
        writer.println("  }");
        writer.println();
    }

    @Override
    protected void genUniMethod(boolean decl, ClassModel model, MethodInfo method, PrintWriter writer) {
        MethodInfo uniMethod = genUniMethodInfo(method);
        ClassTypeInfo raw = uniMethod.getReturnType().getRaw();
        String methodSimpleName = raw.getSimpleName();
        String adapterType = AsyncResultUni.class.getName() + ".to" + methodSimpleName;
        startMethodTemplate(false, uniMethod.getName(), uniMethod,
                new MethodDescriptor("", false, false, true, false),
                writer);
        if (decl) {
            writer.println(";");
            return;
        }
        writer.println(" { ");
        writer.print("    return ");
        writer.print(adapterType);
        writer.println("(handler -> {");
        writer.print("      __");
        writer.print(method.getName());
        writer.print("(");
        List<ParamInfo> params = uniMethod.getParams();
        writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
        if (params.size() > 0) {
            writer.print(", ");
        }
        writer.println("handler);");
        writer.println("    });");
        writer.println("  }");
        writer.println();
    }

    @Override
    protected void genUniMethodForOther(boolean decl, ClassModel model, MethodInfo method, PrintWriter writer) {
        TypeInfo itemType = ((ParameterizedTypeInfo) method.getReturnType()).getArg(0);
        TypeInfo uniReturnType = new io.vertx.codegen.type.ParameterizedTypeInfo(
                io.vertx.codegen.type.TypeReflectionFactory.create(Uni.class).getRaw(),
                true, Collections.singletonList(itemType));
        MethodInfo uniMethod = method.copy().setReturnType(uniReturnType);

        startMethodTemplate(false, uniMethod.getName(), uniMethod,
                new MethodDescriptor("", false, false, true, false),
                writer);
        if (decl) {
            writer.println(";");
            return;
        }
        writer.println(" { ");
        writer.print("    return " + UniHelper.class.getName() + ".toUni(delegate.");
        writer.print(method.getName());
        writer.print("(");
        List<ParamInfo> params = uniMethod.getParams();
        writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
        if (itemType.getKind() == API) {
            writer.print(").map(x -> newInstance(x)));");
        } else {
            writer.print("));");
        }
        writer.println("}");
        writer.println("");
    }

    @Override
    protected void genAndAwaitMethodForOther(boolean decl, ClassModel model, MethodInfo method, PrintWriter writer) {
        TypeInfo itemType = ((ParameterizedTypeInfo) method.getReturnType()).getArg(0);
        if (itemType.getName().equalsIgnoreCase("Void")) {
            itemType = VoidTypeInfo.INSTANCE;
        }
        MethodInfo uniMethod = method.copy().setReturnType(itemType);

        startMethodTemplate(false, uniMethod.getName() + "AndAwait", uniMethod,
                new MethodDescriptor("", false, true, false, false),
                writer);
        if (decl) {
            writer.println(";");
            return;
        }
        writer.println(" { ");
        writer.print("    return " + UniHelper.class.getName() + ".toUni(delegate.");
        writer.print(method.getName());
        writer.print("(");
        List<ParamInfo> params = uniMethod.getParams();
        writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
        if (itemType.getKind() == API) {
            writer.print(").map(x -> newInstance(x))).await().indefinitely();\n");
        } else {
            writer.print(")).await().indefinitely();\n");
        }
        writer.println("  }");
        writer.println("");
    }

    @Override
    protected void genAndForgetMethodForOther(boolean decl, ClassModel model, MethodInfo method, PrintWriter writer) {
        MethodInfo newMethod = method.copy().setReturnType(VoidTypeInfo.INSTANCE);
        Optional<TypeInfo> fluentType = getFluentType(model, method.getName());
        boolean fluent = false;
        if (fluentType.isPresent()) {
            fluent = true;
            newMethod.setReturnType(fluentType.get());
        }


        startMethodTemplate(false, newMethod.getName() + "AndForget", newMethod,
                new MethodDescriptor("", true, false, false, fluent),
                writer);
        if (decl) {
            writer.println(";");
            return;
        }
        writer.println(" { ");
        writer.print("    " + UniHelper.class.getName() + ".toUni(delegate.");
        writer.print(method.getName());
        writer.print("(");
        List<ParamInfo> params = newMethod.getParams();
        writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
        writer.print(")).subscribe().with(x -> {});\n");
        if (fluent) {
            writer.print("    return this;\n");
        }
        writer.println("  }");
        writer.println("");
    }

    /**
     * Checks if there is a method with the same name annotated with {@code @Fluent} in the same class.
     *
     * @param model the class
     * @param name  the method name
     * @return a {@link Optional} wrapping the fluent type if any.
     */
    private Optional<TypeInfo> getFluentType(ClassModel model, String name) {
        return model.getMethods().stream()
                .filter(m -> m.getName().equalsIgnoreCase(name) && m.isFluent())
                .map(MethodInfo::getReturnType)
                .findFirst();
    }

    @Override
    protected void genAndAwaitMethod(boolean decl, ClassModel model, MethodInfo method, PrintWriter writer) {
        MethodInfo blockingMethod = genBlockingMethodInfo(method);
        startMethodTemplate(false, blockingMethod.getName() + "AndAwait", blockingMethod,
                new MethodDescriptor("", false, true, false, false),
                writer);
        if (decl) {
            writer.println(";");
            return;
        }
        writer.println(" { ");
        writer.print("    return (" + CodeGenHelper.genTranslatedTypeName(blockingMethod.getReturnType()) + ") ");
        writer.print(blockingMethod.getName());
        writer.print("(");
        List<ParamInfo> params = blockingMethod.getParams();
        writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
        writer.println(").await().indefinitely();\n");
        writer.println("  }");
        writer.println();
    }

    public MethodInfo genConsumerMethodInfo(MethodInfo method) {
        List<ParamInfo> futParams = new ArrayList<>();
        int count = 0;
        int size = method.getParams().size() - 1;
        while (count < size) {
            ParamInfo param = method.getParam(count);
            futParams.add(param);
            count = count + 1;
        }
        ParamInfo futParam = method.getParam(size);
        TypeInfo consumerType = ((ParameterizedTypeInfo) futParam.getType()).getArg(0);
        TypeInfo consumerUnresolvedType = ((ParameterizedTypeInfo) futParam.getUnresolvedType()).getArg(0);
        TypeInfo consumerReturnType = new io.vertx.codegen.type.ParameterizedTypeInfo(
                io.vertx.codegen.type.TypeReflectionFactory.create(Consumer.class).getRaw(),
                consumerUnresolvedType.isNullable(), Collections.singletonList(consumerType));
        futParams.add(new ParamInfo(futParams.size(), futParam.getName(), futParam.getDescription(),
                consumerReturnType));
        return method.copy().setParams(futParams);
    }

    private MethodInfo genUniMethodInfo(MethodInfo method) {
        List<ParamInfo> params = new ArrayList<>();
        int count = 0;
        int size = method.getParams().size() - 1;
        while (count < size) {
            ParamInfo param = method.getParam(count);
            params.add(param);
            count = count + 1;
        }
        ParamInfo pi = method.getParam(size);
        TypeInfo uniType = ((ParameterizedTypeInfo) ((ParameterizedTypeInfo) pi.getType()).getArg(0)).getArg(0);
        TypeInfo uniUnresolvedType = ((ParameterizedTypeInfo) ((ParameterizedTypeInfo) pi.getUnresolvedType())
                .getArg(0))
                .getArg(0);
        TypeInfo uniReturnType = new io.vertx.codegen.type.ParameterizedTypeInfo(
                io.vertx.codegen.type.TypeReflectionFactory.create(Uni.class).getRaw(),
                uniUnresolvedType.isNullable(), Collections.singletonList(uniType));
        return method.copy().setReturnType(uniReturnType).setParams(params);
    }

    private MethodInfo genBlockingMethodInfo(MethodInfo method) {
        List<ParamInfo> futParams = new ArrayList<>();
        int count = 0;
        int size = method.getParams().size() - 1;
        while (count < size) {
            ParamInfo param = method.getParam(count);
            futParams.add(param);
            count = count + 1;
        }
        ParamInfo futParam = method.getParam(size);
        TypeInfo futType = ((ParameterizedTypeInfo) ((ParameterizedTypeInfo) futParam.getType()).getArg(0)).getArg(0);
        return method.copy().setReturnType(futType).setParams(futParams);
    }

    private MethodInfo genForgetMethodInfo(MethodInfo method) {
        List<ParamInfo> futParams = new ArrayList<>();
        int count = 0;
        int size = method.getParams().size() - 1;
        while (count < size) {
            ParamInfo param = method.getParam(count);
            futParams.add(param);
            count = count + 1;
        }
        return method.copy().setReturnType(VoidTypeInfo.INSTANCE).setParams(futParams);
    }

    private MethodInfo genOverloadedMethod(MethodInfo method, Class streamType) {
        List<ParamInfo> params = null;
        int count = 0;
        for (ParamInfo param : method.getParams()) {
            if (param.getType().isParameterized()
                    && param.getType().getRaw().getName().equals("io.vertx.core.streams.ReadStream")) {
                if (params == null) {
                    params = new ArrayList<>(method.getParams());
                }
                ParameterizedTypeInfo paramType = new io.vertx.codegen.type.ParameterizedTypeInfo(
                        io.vertx.codegen.type.TypeReflectionFactory.create(streamType).getRaw(),
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
}
