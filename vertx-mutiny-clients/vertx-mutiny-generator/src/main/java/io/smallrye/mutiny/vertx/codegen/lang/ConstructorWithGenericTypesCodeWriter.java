package io.smallrye.mutiny.vertx.codegen.lang;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.Helper;
import io.vertx.codegen.TypeParamInfo;

import java.io.PrintWriter;
import java.util.List;

public class ConstructorWithGenericTypesCodeWriter implements ConditionalCodeWriter {
    private final String constructor;

    public ConstructorWithGenericTypesCodeWriter(String constructor) {
        this.constructor = constructor;
    }

    public ConstructorWithGenericTypesCodeWriter() {
        this.constructor = null;
    }

    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        String cst = constructor;
        if (cst == null) {
            cst = model.getIfaceSimpleName();
        }
        List<TypeParamInfo.Class> typeParams = model.getTypeParams();
        if (typeParams.size() > 0) {
            writer.print("  public ");
            writer.print(cst);
            writer.print("(");
            writer.print(Helper.getNonGenericType(model.getIfaceFQCN()));
            writer.print(" delegate");
            for (TypeParamInfo.Class typeParam : typeParams) {
                writer.print(", io.smallrye.mutiny.vertx.TypeArg<");
                writer.print(typeParam.getName());
                writer.print("> typeArg_");
                writer.print(typeParam.getIndex());
            }
            writer.println(") {");
            if (model.isConcrete() && CodeGenHelper.hasParentClass(model)) {
                writer.println("    super(delegate);");
            }
            writer.println("    this.delegate = delegate;");
            for (TypeParamInfo.Class typeParam : typeParams) {
                writer.print("    this.__typeArg_");
                writer.print(typeParam.getIndex());
                writer.print(" = typeArg_");
                writer.print(typeParam.getIndex());
                writer.println(";");
            }
            writer.println("  }");
            writer.println();
        }
    }

    @Override
    public boolean test(ClassModel classModel) {
        return classModel.isConcrete();
    }
}
