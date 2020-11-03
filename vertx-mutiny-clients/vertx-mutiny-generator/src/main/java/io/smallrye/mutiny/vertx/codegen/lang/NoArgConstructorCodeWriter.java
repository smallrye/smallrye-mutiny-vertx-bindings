package io.smallrye.mutiny.vertx.codegen.lang;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.TypeParamInfo;

import java.io.PrintWriter;
import java.util.List;

public class NoArgConstructorCodeWriter implements ConditionalCodeWriter {
    private String constructor;

    public NoArgConstructorCodeWriter(String constructor) {
        this.constructor = constructor;
    }

    public NoArgConstructorCodeWriter() {
        this.constructor = null;
    }

    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        String cst = constructor;
        if (cst == null) {
            cst = model.getIfaceSimpleName();
        }

        List<TypeParamInfo.Class> typeParams = model.getTypeParams();
        // Constructor without parameter, used by CDI
        writer.println("  /**");
        writer.println("  * Empty constructor used by CDI, do not use this constructor directly.");
        writer.println("  **/");
        writer.print("  ");
        writer.print(cst);
        writer.print("() {");
        if (model.isConcrete() && CodeGenHelper.hasParentClass(model)) {
            writer.println("    super(null);");
        }
        writer.println("    this.delegate = null;");
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
