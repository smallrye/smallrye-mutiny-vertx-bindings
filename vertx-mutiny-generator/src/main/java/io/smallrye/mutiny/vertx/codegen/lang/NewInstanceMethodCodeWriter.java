package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.type.ClassTypeInfo;

public class NewInstanceMethodCodeWriter implements CodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        ClassTypeInfo type = model.getType();
        writer.print("  public static ");
        writer.print(CodeGenHelper.genOptTypeParamsDecl(type, " "));
        writer.print(type.getSimpleName());
        writer.print(CodeGenHelper.genOptTypeParamsDecl(type, ""));
        writer.print(" newInstance(");
        writer.print(type.getName());
        writer.println(" arg) {");

        writer.print("    return arg != null ? new ");
        writer.print(type.getSimpleName());
        if (!model.isConcrete()) {
            writer.print("Impl");
        }
        writer.print(CodeGenHelper.genOptTypeParamsDecl(type, ""));
        writer.println("(arg) : null;");
        writer.println("  }");
        writer.println();
    }
}
