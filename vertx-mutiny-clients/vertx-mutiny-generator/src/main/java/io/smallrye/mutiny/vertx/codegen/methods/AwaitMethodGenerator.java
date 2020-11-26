package io.smallrye.mutiny.vertx.codegen.methods;

import io.smallrye.mutiny.vertx.UniHelper;
import io.smallrye.mutiny.vertx.codegen.lang.CodeGenHelper;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.MethodInfo;
import io.vertx.codegen.ParamInfo;
import io.vertx.codegen.type.ParameterizedTypeInfo;
import io.vertx.codegen.type.TypeInfo;
import io.vertx.codegen.type.VoidTypeInfo;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.vertx.codegen.type.ClassKind.API;

public class AwaitMethodGenerator extends MutinyMethodGenerator {

    public static final String SUFFIX_AND_AWAIT = "AndAwait";

    public AwaitMethodGenerator(PrintWriter writer) {
        super(writer);
    }

    public void generate(MethodInfo method) {
        MutinyMethodDescriptor awaitMethod = computeMethodInfo(method);
        generateJavadoc(awaitMethod);
        generateMethodDeclaration(awaitMethod);
        generateBody(awaitMethod);
        writer.println();
    }

    public void generateDeclaration(MethodInfo method) {
        MutinyMethodDescriptor awaitMethod = computeMethodInfo(method);
        generateJavadoc(awaitMethod);
        generateMethodDeclaration(awaitMethod);
        writer.println(";");
        writer.println();
    }

    public void generateOther(MethodInfo method) {
        MutinyMethodDescriptor awaitMethod = computeMethodInfoOther(method);
        generateJavadoc(awaitMethod);
        generateMethodDeclaration(awaitMethod);
        generateBodyOther(awaitMethod);
        writer.println();
    }

    private void generateBody(MutinyMethodDescriptor descriptor) {
        MethodInfo method = descriptor.getMethod();
        MethodInfo original = descriptor.getOriginalMethod();
        writer.println(" { ");
        if (! descriptor.getMethod().getReturnType().isVoid()) {
            writer.print("    return (" + CodeGenHelper.genTranslatedTypeName(method.getReturnType()) + ") ");
        }
        writer.print(original.getName());
        writer.print("(");
        List<ParamInfo> params = method.getParams();
        writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
        writer.println(").await().indefinitely();");
        writer.println("  }");
    }

    private void generateBodyOther(MutinyMethodDescriptor method) {
        writer.println(" { ");
        if (! method.getMethod().getReturnType().isVoid()) {
            writer.print("    return " + UniHelper.class.getName() + ".toUni(delegate.");
        }
        writer.print(method.getOriginalMethodName());
        writer.print("(");
        List<ParamInfo> params = method.getMethod().getParams();
        writer.print(params.stream().map(pi -> {
            if (pi.getType().getKind() == API) {
                return pi.getName() + ".getDelegate()";
            } else {
                return pi.getName();
            }
        }).collect(Collectors.joining(", ")));
        if (method.getMethod().getReturnType().getKind() == API) {
            writer.println(").map(x -> newInstance(x))).await().indefinitely();");
        } else {
            writer.println(")).await().indefinitely();");
        }
        writer.println("  }");
        writer.println("");
    }

    private MutinyMethodDescriptor computeMethodInfoOther(MethodInfo method) {
        TypeInfo itemType = ((ParameterizedTypeInfo) method.getReturnType()).getArg(0);
        MethodInfo newMethod = method.copy().setReturnType(itemType)
                .setName(method.getName() + SUFFIX_AND_AWAIT);

        return new MutinyMethodDescriptor(newMethod, method, MutinyMethodDescriptor.MutinyKind.AWAIT);
    }

    private MutinyMethodDescriptor computeMethodInfo(MethodInfo method) {
        List<ParamInfo> params = new ArrayList<>(method.getParams());
        // The last parameter is the Handler<AsyncResult<T>> - removing it.
        ParamInfo handler = params.remove(method.getParams().size() - 1);
        // Extract the <T> -> It's the return type
        TypeInfo returnType = ((ParameterizedTypeInfo) ((ParameterizedTypeInfo) handler.getType()).getArg(0)).getArg(0);
        return new MutinyMethodDescriptor(method.copy()
                .setName(method.getName() + SUFFIX_AND_AWAIT)
                .setReturnType(returnType).setParams(params), method, MutinyMethodDescriptor.MutinyKind.AWAIT);
    }


}
