package io.smallrye.mutiny.vertx.codegen.lang;

import io.smallrye.mutiny.vertx.TypeArg;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.Helper;
import io.vertx.codegen.TypeParamInfo;

import java.io.PrintWriter;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class DelegateFieldCodeWriter implements ConditionalCodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        writer.print("  private final ");
        writer.print(Helper.getNonGenericType(model.getIfaceFQCN()));
        List<TypeParamInfo.Class> typeParams = model.getTypeParams();
        if (typeParams.size() > 0) {
            writer.print(typeParams.stream().map(TypeParamInfo.Class::getName).collect(joining(",", "<", ">")));
        }
        writer.println(" delegate;");

        for (TypeParamInfo.Class typeParam : typeParams) {
            writer.print("  public final " + TypeArg.class.getName() + "<");
            writer.print(typeParam.getName());
            writer.print("> __typeArg_");
            writer.print(typeParam.getIndex());
            writer.println(";");
        }
        writer.println("  ");
    }

    @Override
    public boolean test(ClassModel classModel) {
        return classModel.isConcrete();
    }
}
