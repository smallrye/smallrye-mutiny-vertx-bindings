package io.smallrye.mutiny.vertx.codegen.lang;

import io.smallrye.mutiny.vertx.TypeArg;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.TypeParamInfo;
import io.vertx.codegen.type.ClassTypeInfo;

import java.io.PrintWriter;

import static io.smallrye.mutiny.vertx.codegen.lang.CodeGenHelper.genOptTypeParamsDecl;

public class NewInstanceWithGenericsMethodCodeWriter implements ConditionalCodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        ClassTypeInfo type = model.getType();
        writer.println();
        writer.print("  public static ");
        writer.print(genOptTypeParamsDecl(type, " "));
        writer.print(type.getSimpleName());
        writer.print(genOptTypeParamsDecl(type, ""));
        writer.print(" newInstance(");
        writer.print(type.getName());
        writer.print(" arg");
        for (TypeParamInfo typeParam : type.getParams()) {
            writer.print(", " + TypeArg.class.getName() + "<");
            writer.print(typeParam.getName());
            writer.print("> __typeArg_");
            writer.print(typeParam.getName());
        }
        writer.println(") {");

        writer.print("    return arg != null ? new ");
        writer.print(type.getSimpleName());
        if (!model.isConcrete()) {
            writer.print("Impl");
        }
        writer.print(genOptTypeParamsDecl(type, ""));
        writer.print("(arg");
        for (TypeParamInfo typeParam : type.getParams()) {
            writer.print(", __typeArg_");
            writer.print(typeParam.getName());
        }
        writer.println(") : null;");
        writer.println("  }");
        writer.println();
    }

    @Override
    public boolean test(ClassModel classModel) {
        return !classModel.getType().getParams().isEmpty();
    }
}
