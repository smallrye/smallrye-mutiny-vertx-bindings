package io.smallrye.mutiny.vertx.codegen.lang;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.Helper;
import io.vertx.codegen.TypeParamInfo;

import java.io.PrintWriter;
import java.util.List;

public class ConstructorWithDelegateParameterCodeWriter implements ConditionalCodeWriter {

    private final String constructor;

    public ConstructorWithDelegateParameterCodeWriter(String constructor) {
        this.constructor = constructor;
    }

    public ConstructorWithDelegateParameterCodeWriter() {
        this.constructor = null;
    }

    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        // Constructor taking delegate as parameter.
        String cst = constructor;
        if (cst == null) {
            cst = model.getIfaceSimpleName();
        }

        List<TypeParamInfo.Class> typeParams = model.getTypeParams();
        writer.print("  public ");
        writer.print(cst);
        writer.print("(");
        writer.print(Helper.getNonGenericType(model.getIfaceFQCN()));
        writer.println(" delegate) {");

        if (model.isConcrete() && CodeGenHelper.hasParentClass(model)) {
            writer.println("    super(delegate);");
        }
        writer.println("    this.delegate = delegate;");
        for (TypeParamInfo.Class typeParam : typeParams) {
            writer.print("    this.__typeArg_");
            writer.print(typeParam.getIndex());
            writer.print(" = io.smallrye.mutiny.vertx.TypeArg.unknown();");
        }
        writer.println("  }");
        writer.println();
    }

    @Override
    public boolean test(ClassModel classModel) {
        return classModel.isConcrete();
    }
}
