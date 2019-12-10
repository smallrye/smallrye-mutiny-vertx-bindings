package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.TypeInfo;

public class ToMultiMethodCodeWriter implements ConditionalCodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        writer.print("  private Multi<");
        writer.print(genTypeName(model.getReadStreamArg()));
        writer.println("> multi;");

        genToMulti(model.getReadStreamArg(), "multi", writer);
    }

    @Override
    public boolean test(ClassModel classModel) {
        return classModel.isReadStream()  && classModel.isConcrete();
    }

    private void genToMulti(TypeInfo type, String fieldName, PrintWriter writer) {
        writer.print("  public synchronized Multi");
        writer.print("<");
        writer.print(genTypeName(type));
        writer.print("> toMulti");
        writer.println("() {");

        writer.print("    ");
        writer.print("if (");
        writer.print(fieldName);
        writer.println(" == null) {");

        if (type.getKind() == ClassKind.API) {
            writer.print("      java.util.function.Function<");
            writer.print(type.getName());
            writer.print(", ");
            writer.print(genTypeName(type));
            writer.print("> conv = ");
            writer.print(genTypeName(type.getRaw()));
            writer.println("::newInstance;");

            writer.print("      ");
            writer.print(fieldName);
            writer.print(" = io.smallrye.mutiny.vertx.MultiHelper.toMulti(delegate, conv);");
        } else if (type.isVariable()) {
            String typeVar = type.getSimpleName();
            writer.print("      java.util.function.Function<");
            writer.print(typeVar);
            writer.print(", ");
            writer.print(typeVar);
            writer.print("> conv = (java.util.function.Function<");
            writer.print(typeVar);
            writer.print(", ");
            writer.print(typeVar);
            writer.println(">) __typeArg_0.wrap;");

            writer.print("      ");
            writer.print(fieldName);
            writer.print(" = io.smallrye.mutiny.vertx.MultiHelper.toMulti(delegate, conv);");
        } else {
            writer.print("      ");
            writer.print(fieldName);
            writer.print(" = io.smallrye.mutiny.vertx.MultiHelper.toMulti(this.getDelegate());");
        }

        writer.println("    }");
        writer.print("    return ");
        writer.print(fieldName);
        writer.println(";");
        writer.println("  }");
        writer.println();
    }

}
