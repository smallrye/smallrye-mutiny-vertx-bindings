package io.smallrye.mutiny.vertx.codegen.lang;

import io.smallrye.mutiny.vertx.codegen.AbstractMutinyGenerator;
import io.vertx.codegen.ClassModel;

import java.io.PrintWriter;

public class PackageDeclarationCodeWriter implements CodeWriter {

    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        writer.print("package ");
        writer.print(model.getType().translatePackageName(AbstractMutinyGenerator.ID));
        writer.println(";");
        writer.println();
    }
}
