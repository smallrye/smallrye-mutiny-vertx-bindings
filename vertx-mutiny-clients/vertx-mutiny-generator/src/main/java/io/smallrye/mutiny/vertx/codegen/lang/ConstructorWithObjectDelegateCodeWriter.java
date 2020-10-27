package io.smallrye.mutiny.vertx.codegen.lang;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.Helper;
import io.vertx.codegen.TypeParamInfo;

import java.io.PrintWriter;
import java.util.List;

public class ConstructorWithObjectDelegateCodeWriter implements ConditionalCodeWriter {

    private final String constructor;

    public ConstructorWithObjectDelegateCodeWriter(String constructor) {
        this.constructor = constructor;
    }

    public ConstructorWithObjectDelegateCodeWriter() {
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
        writer.print("(Object delegate");
        for (TypeParamInfo.Class typeParam : typeParams) {
            writer.print(", TypeArg<");
            writer.print(typeParam.getName());
            writer.print("> typeArg_");
            writer.print(typeParam.getIndex());
        }
        writer.println(") {");
        if (model.getConcreteSuperType() != null) {
            // This is incorrect it will not pass the generic type in some case
            // we haven't yet ran into that bug
            writer.print("    super((");
            writer.print(Helper.getNonGenericType(model.getIfaceFQCN()));
            writer.println(")delegate);");
        }
        writer.print("    this.delegate = (");
        writer.print(Helper.getNonGenericType(model.getIfaceFQCN()));
        writer.println(")delegate;");
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

    @Override
    public boolean test(ClassModel classModel) {
        return classModel.isConcrete();
    }
}
