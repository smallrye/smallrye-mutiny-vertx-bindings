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
        writer.println("> multi;\n");

        genToMulti(model.getReadStreamArg(), writer);
        genToBlockingIterable(model.getReadStreamArg(), writer);
        genToBlockingStream(model.getReadStreamArg(), writer);
    }

    @Override
    public boolean test(ClassModel classModel) {
        return classModel.isReadStream()  && classModel.isConcrete();
    }

    private void genToMulti(TypeInfo type, PrintWriter writer) {
        writer.print("  @CheckReturnValue\n");
        writer.print("  public synchronized Multi");
        writer.print("<");
        writer.print(genTypeName(type));
        writer.print("> toMulti");
        writer.println("() {");

        writer.print("    ");
        writer.print("if (");
        writer.print("multi");
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
            writer.print("multi");
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
            writer.print("multi");
            writer.print(" = io.smallrye.mutiny.vertx.MultiHelper.toMulti(delegate, conv);");
        } else {
            writer.print("      ");
            writer.print("multi");
            writer.print(" = io.smallrye.mutiny.vertx.MultiHelper.toMulti(this.getDelegate());");
        }

        writer.println("    }");
        writer.print("    return ");
        writer.print("multi");
        writer.println(";");
        writer.println("  }");
        writer.println();
    }

    private void genToBlockingIterable(TypeInfo type, PrintWriter writer) {
        writer.print("  public java.lang.Iterable");
        writer.print("<");
        writer.print(genTypeName(type));
        writer.print("> toBlockingIterable");
        writer.println("() {");

        writer.print("    ");
        writer.print("return toMulti().subscribe().asIterable();\n");
        writer.println("  }");
        writer.println();
    }

    private void genToBlockingStream(TypeInfo type, PrintWriter writer) {
        writer.print("  public java.util.stream.Stream");
        writer.print("<");
        writer.print(genTypeName(type));
        writer.print("> toBlockingStream");
        writer.println("() {");

        writer.print("    ");
        writer.print("return toMulti().subscribe().asStream();\n");
        writer.println("  }");
        writer.println();
    }

}
