package io.smallrye.mutiny.vertx.codegen.lang;

import io.smallrye.mutiny.vertx.TypeArg;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.type.ClassTypeInfo;

import java.io.PrintWriter;

public class TypeArgsConstantCodeWriter implements ConditionalCodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        ClassTypeInfo type = model.getType();
        String simpleName = type.getSimpleName();

        writer.print("  public static final " + TypeArg.class.getName() + "<");
        writer.print(simpleName);
        writer.print("> __TYPE_ARG = new " + TypeArg.class.getName() + "<>(");
        writer.print("    obj -> new ");
        writer.print(simpleName);
        writer.print("((");
        writer.print(type.getName());
        writer.println(") obj),");
        writer.print("    ");
        writer.print(simpleName);
        writer.println("::getDelegate");
        writer.println("  );");
        writer.println();
    }

    @Override
    public boolean test(ClassModel classModel) {
        return classModel.isConcrete();
    }
}
