package io.smallrye.mutiny.vertx.codegen.lang;

import io.smallrye.mutiny.vertx.codegen.MutinyGenerator;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.Helper;
import io.vertx.codegen.type.ClassTypeInfo;

import java.io.PrintWriter;

public class ImplClassCodeWriter implements ConditionalCodeWriter {
    private final MutinyGenerator generator;

    public ImplClassCodeWriter(MutinyGenerator mutinyGenerator) {
        this.generator = mutinyGenerator;
    }

    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        ClassTypeInfo type = model.getType();
        writer.println();
        writer.print("class ");
        writer.print(type.getSimpleName());
        writer.print("Impl");
        writer.print(CodeGenHelper.genOptTypeParamsDecl(type, ""));
        writer.print(" implements ");
        writer.print(Helper.getSimpleName(model.getIfaceFQCN()));
        writer.println(" {");

        // By-pass conditions
        new DelegateFieldCodeWriter().generate(model, writer);
        new GetDelegateMethodCodeWriter().generate(model, writer);
        new NoArgConstructorCodeWriter(type.getSimpleName() + "Impl").generate(model, writer);
        new ConstructorWithDelegateParameterCodeWriter(type.getSimpleName() + "Impl").generate(model, writer);
        new ConstructorWithGenericTypesCodeWriter(type.getSimpleName() + "Impl").generate(model, writer);
        if (model.isReadStream()) {
            new ToMultiMethodCodeWriter().generate(model, writer);
        }

        generator.generateClassBody(model, writer);
        writer.println("}");
    }

    @Override
    public boolean test(ClassModel classModel) {
        return !classModel.isConcrete();
    }
}
