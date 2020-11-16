package io.smallrye.mutiny.vertx.codegen.methods;

import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.MethodInfo;
import io.vertx.codegen.ParamInfo;
import io.vertx.codegen.type.TypeInfo;
import io.vertx.codegen.type.VoidTypeInfo;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ForgetMethodGenerator extends MutinyMethodGenerator {

    public static final String SUFFIX_AND_FORGET = "AndForget";

    public ForgetMethodGenerator(PrintWriter writer) {
        super(writer);
    }

    public void generate(ClassModel model, MethodInfo method) {
        MutinyMethodDescriptor forgetMethod = computeMethodInfo(model, method);
        generateJavadoc(forgetMethod);
        generateMethodDeclaration(forgetMethod);
        generateBody(forgetMethod);
        writer.println();
    }

    public void generateDeclaration(ClassModel model, MethodInfo method) {
        MutinyMethodDescriptor forgetMethod = computeMethodInfo(model, method);
        generateJavadoc(forgetMethod);
        generateMethodDeclaration(forgetMethod);
        writer.println(";");
        writer.println();
    }

    public void generateOther(ClassModel model, MethodInfo method) {
        MutinyMethodDescriptor forgetMethod = computeMethodInfoOther(model, method);
        generateJavadoc(forgetMethod);
        generateMethodDeclaration(forgetMethod);
        generateBodyOther(forgetMethod);
        writer.println();
    }

    private void generateBody(MutinyMethodDescriptor forgetMethod) {
        writer.println(" { ");
        writer.print("    " + forgetMethod.getOriginalMethodName());
        writer.print("(");
        List<ParamInfo> params = forgetMethod.getMethod().getParams();
        writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
        writer.println(").subscribe().with(x -> {});");
        if (forgetMethod.isFluent()) {
            writer.println("    return this;");
        }
        writer.println("  }");
    }

    private void generateBodyOther(MutinyMethodDescriptor forgetMethod) {
        writer.println(" { ");
        writer.print("    " + UniHelper.class.getName() + ".toUni(delegate.");
        writer.print(forgetMethod.getOriginalMethodName());
        writer.print("(");
        List<ParamInfo> params = forgetMethod.getMethod().getParams();
        writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
        writer.print(")).subscribe().with(x -> {});\n");
        if (forgetMethod.isFluent()) {
            writer.print("    return this;\n");
        }
        writer.println("  }");
        writer.println("");
    }

    private MutinyMethodDescriptor computeMethodInfoOther(ClassModel model, MethodInfo method) {
        MethodInfo newMethod = method.copy().setReturnType(VoidTypeInfo.INSTANCE)
                .setName(method.getName() + SUFFIX_AND_FORGET);
        Optional<TypeInfo> fluentType = getFluentType(model, method.getName());
        boolean fluent = false;
        if (fluentType.isPresent()) {
            fluent = true;
            newMethod.setReturnType(fluentType.get());
        }
        return MutinyMethodDescriptor.createAndForgetMethod(newMethod, method, fluent);
    }

    private MutinyMethodDescriptor computeMethodInfo(ClassModel model, MethodInfo method) {
        List<ParamInfo> params = new ArrayList<>(method.getParams());
        TypeInfo returnType = VoidTypeInfo.INSTANCE;
        Optional<TypeInfo> fluentType = getFluentType(model, method.getName());
        boolean fluent = false;
        if (fluentType.isPresent()) {
            fluent = true;
            returnType = fluentType.get();
        }

        // The last parameter is the Handler<AsyncResult<T>> - removing it.
        params.remove(method.getParams().size() - 1);
        return MutinyMethodDescriptor.createAndForgetMethod(method.copy()
                .setName(method.getName() + SUFFIX_AND_FORGET)
                .setReturnType(returnType).setParams(params), method, fluent);
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


}
