package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;
import java.util.List;

import io.smallrye.mutiny.Multi;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.TypeParamInfo;

public class ReadStreamMethodDeclarationCodeWriter implements ConditionalCodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        List<TypeParamInfo.Class> params = model.getType().getParams();
        writer.print("  @CheckReturnValue\n");
        writer.print("  " + Multi.class.getName() + "<");
        writer.print(params.get(0).getName());
        writer.println("> toMulti();");
        writer.println();
    }

    @Override
    public boolean test(ClassModel classModel) {
        return !classModel.isConcrete()
                && classModel.getType().getRaw().getName().equals("io.vertx.core.streams.ReadStream");
    }
}
