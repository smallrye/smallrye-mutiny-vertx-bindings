package io.smallrye.mutiny.vertx.codegen.methods;

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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.vertx.codegen.type.ClassKind.API;

public class UniMethodGenerator extends MutinyMethodGenerator {

    public UniMethodGenerator(PrintWriter writer) {
        super(writer);
    }

    public void generate(MethodInfo method) {
        MutinyMethodDescriptor uniMethod = computeMethodInfo(method);
        generateJavadoc(uniMethod);
        generateMethodDeclaration(uniMethod);
        generateBody(uniMethod);
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

    private void generateBody(MutinyMethodDescriptor descriptor) {
        MethodInfo method = descriptor.getMethod();

        ClassTypeInfo raw = method.getReturnType().getRaw();
        String methodSimpleName = raw.getSimpleName();
        String adapterType = AsyncResultUni.class.getName() + ".to" + methodSimpleName;

        writer.println(" { ");
        writer.print("    return ");
        writer.print(adapterType);
        writer.println("(handler -> {");
        // TODO Inline method body here.
        writer.print("      __" + method.getName() + "(");
        List<ParamInfo> params = method.getParams();
        writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
        if (params.size() > 0) {
            writer.print(", ");
        }
        writer.println("handler);");
        writer.println("    });");
        writer.println("  }");
    }

    private void generateBodyOther(MutinyMethodDescriptor descriptor) {
        MethodInfo method = descriptor.getMethod();

        writer.println(" { ");
        writer.print("    return " + UniHelper.class.getName() + ".toUni(delegate.");
        writer.print(method.getName());
        writer.print("(");
        List<ParamInfo> params = method.getParams();
        writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
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
