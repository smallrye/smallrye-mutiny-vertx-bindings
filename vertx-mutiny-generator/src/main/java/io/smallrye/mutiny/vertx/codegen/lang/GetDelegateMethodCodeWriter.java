package io.smallrye.mutiny.vertx.codegen.lang;

import io.vertx.codegen.ClassModel;

import java.io.PrintWriter;

public class GetDelegateMethodCodeWriter implements ConditionalCodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        writer.println("  @Override");
        writer.print("  public ");
        writer.print(model.getType().getName());
        writer.println(" getDelegate() {");
        writer.println("    return delegate;");
        writer.println("  }");
        writer.println();
    }

    @Override
    public boolean test(ClassModel classModel) {
        return classModel.isConcrete();
    }
}
