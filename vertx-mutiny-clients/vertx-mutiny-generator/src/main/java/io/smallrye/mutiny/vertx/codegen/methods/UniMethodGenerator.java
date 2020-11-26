package io.smallrye.mutiny.vertx.codegen.methods;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.AsyncResultUni;
import io.smallrye.mutiny.vertx.ReadStreamSubscriber;
import io.smallrye.mutiny.vertx.UniHelper;
import io.smallrye.mutiny.vertx.codegen.lang.CodeGenHelper;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.Helper;
import io.vertx.codegen.MethodInfo;
import io.vertx.codegen.ParamInfo;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.ClassTypeInfo;
import io.vertx.codegen.type.ParameterizedTypeInfo;
import io.vertx.codegen.type.TypeInfo;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.vertx.codegen.type.ClassKind.API;

public class UniMethodGenerator extends MutinyMethodGenerator {

    private final Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap;

    public UniMethodGenerator(PrintWriter writer,
            Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap) {
        super(writer);
        this.methodTypeArgMap = methodTypeArgMap;
    }

    public void generate(ClassModel model, MethodInfo method) {
        MutinyMethodDescriptor uniMethod = computeMethodInfo(method);
        generateJavadoc(uniMethod);
        generateMethodDeclaration(uniMethod);
        generateBody(model, uniMethod);
        writer.println();
    }

    public void generateDeclaration(MethodInfo method) {
        MutinyMethodDescriptor uniMethod = computeMethodInfo(method);
        generateJavadoc(uniMethod);
        generateMethodDeclaration(uniMethod);
        writer.println(";");
        writer.println();
    }

    public void generateOther(MethodInfo method) {
        MutinyMethodDescriptor uniMethod = computeMethodInfoOther(method);
        generateJavadoc(uniMethod);
        generateMethodDeclaration(uniMethod);
        generateBodyOther(uniMethod);
        writer.println();
    }

    private void generateBody(ClassModel model, MutinyMethodDescriptor descriptor) {
        MethodInfo method = descriptor.getMethod();

        ClassTypeInfo raw = method.getReturnType().getRaw();
        String methodSimpleName = raw.getSimpleName();
        String adapterType = AsyncResultUni.class.getName() + ".to" + methodSimpleName;
        List<ParamInfo> params = descriptor.getOriginalMethod().getParams();
        String handlerParameterName = params.get(params.size() - 1).getName();

        writer.println(" { ");
        writer.print("    return ");
        writer.print(adapterType);
        writer.println("(" + handlerParameterName + " -> {");
        writer.println("        " + invokeDelegate(methodTypeArgMap, model, descriptor.getOriginalMethod()) + ";");
        writer.println("    });");
        writer.println("  }");
    }

    public static String invokeDelegate(Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap, ClassModel model, MethodInfo method) {
        StringBuilder object;
        if (method.isStaticMethod()) {
            object = new StringBuilder(Helper.getNonGenericType(model.getIfaceFQCN()));
        } else {
            object = new StringBuilder("delegate");
        }

        object.append(".").append(method.getName()).append("(");
        int index = 0;
        for (ParamInfo param : method.getParams()) {
            if (index > 0) {
                object.append(", ");
            }
            TypeInfo type = param.getType();
            if (type.isParameterized() && (type.getRaw().getName().equals("org.reactivestreams.Publisher"))) {
                String adapterFunction;
                ParameterizedTypeInfo parameterizedType = (ParameterizedTypeInfo) type;
                if (parameterizedType.getArg(0).isVariable()) {
                    adapterFunction = "java.util.function.Function.identity()";
                } else {
                    adapterFunction =
                            "obj -> (" + parameterizedType.getArg(0).getRaw().getName() + ") obj.getDelegate()";
                }
                object.append(ReadStreamSubscriber.class.getName()).append(".asReadStream(")
                        .append(param.getName())
                        .append(",")
                        .append(adapterFunction).append(").resume()");
            } else {
                object.append(CodeGenHelper.genConvParam(methodTypeArgMap, type, method, param.getName()));
            }
            index = index + 1;
        }
        object.append(")");
        return object.toString();
    }

    private void generateBodyOther(MutinyMethodDescriptor descriptor) {
        MethodInfo method = descriptor.getMethod();

        writer.println(" { ");
        writer.print("    return " + UniHelper.class.getName() + ".toUni(delegate.");
        writer.print(method.getName());
        writer.print("(");
        List<ParamInfo> params = method.getParams();
        writer.print(params.stream().map(pi -> {
            if (pi.getType().getKind() == API) {
                return pi.getName() + ".getDelegate()";
            } else {
                return pi.getName();
            }
        }).collect(Collectors.joining(", ")));
        TypeInfo arg = ((ParameterizedTypeInfo) (descriptor.getOriginalMethod().getReturnType())).getArg(0);
        if (arg.getKind() == API) {
            writer.print(").map(x -> newInstance(x)));");
        } else {
            writer.print("));");
        }
        writer.println("}");
    }

    private MutinyMethodDescriptor computeMethodInfoOther(MethodInfo method) {
        TypeInfo itemType = ((ParameterizedTypeInfo) method.getReturnType()).getArg(0);
        TypeInfo uniReturnType = new io.vertx.codegen.type.ParameterizedTypeInfo(
                io.vertx.codegen.type.TypeReflectionFactory.create(Uni.class).getRaw(),
                true, Collections.singletonList(itemType));
        MethodInfo uniMethod = method.copy().setReturnType(uniReturnType);
        return new MutinyMethodDescriptor(uniMethod, method, MutinyMethodDescriptor.MutinyKind.UNI);
    }

    private MutinyMethodDescriptor computeMethodInfo(MethodInfo method) {
        // Remove the last Handler<AsyncResult<T>> parameter
        List<ParamInfo> params = new ArrayList<>(method.getParams());
        ParamInfo handler = params.remove(method.getParams().size() - 1);

        // Extract <T> and build Uni<T>
        TypeInfo uniType = ((ParameterizedTypeInfo) ((ParameterizedTypeInfo) handler.getType()).getArg(0)).getArg(0);
        TypeInfo uniUnresolvedType = ((ParameterizedTypeInfo) ((ParameterizedTypeInfo) handler.getUnresolvedType())
                .getArg(0)).getArg(0);
        TypeInfo uniReturnType = new io.vertx.codegen.type.ParameterizedTypeInfo(
                io.vertx.codegen.type.TypeReflectionFactory.create(Uni.class).getRaw(),
                uniUnresolvedType.isNullable(), Collections.singletonList(uniType));
        MethodInfo newMethod = method.copy().setReturnType(uniReturnType).setParams(params);
        return new MutinyMethodDescriptor(newMethod, method, MutinyMethodDescriptor.MutinyKind.UNI);
    }

}
