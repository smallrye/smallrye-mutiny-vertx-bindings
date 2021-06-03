package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;

import io.vertx.codegen.ClassModel;

public class DelegateMethodDeclarationCodeWriter implements ConditionalCodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        writer.print("  ");
        writer.print(model.getType().getName());
        writer.println(" getDelegate();");
        writer.println();
    }

    @Override
    public boolean test(ClassModel classModel) {
        return !classModel.isConcrete();
    }
}
