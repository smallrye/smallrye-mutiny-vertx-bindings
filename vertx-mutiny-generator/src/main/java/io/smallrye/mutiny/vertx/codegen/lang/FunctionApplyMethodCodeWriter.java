package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.TypeInfo;

public class FunctionApplyMethodCodeWriter implements ConditionalCodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        TypeInfo[] functionArgs = model.getFunctionArgs();
        TypeInfo inArg = functionArgs[0];
        TypeInfo outArg = functionArgs[1];

        writer.println("  @Override");
        writer.printf("  public %s apply(%s in) {%n", genTypeName(outArg), genTypeName(inArg));
        writer.printf("    %s ret;%n", outArg.getName());

        if (inArg.getKind() == ClassKind.API) {
            writer.println("    ret = getDelegate().apply(in.getDelegate());");
        } else if (inArg.isVariable()) {
            String typeVar = inArg.getSimpleName();
            writer.format(
                    "    java.util.function.Function<%s, %s> inConv = (java.util.function.Function<%s, %s>) __typeArg_0.unwrap;%n",
                    typeVar, typeVar, typeVar, typeVar);
            writer.println("    ret = getDelegate().apply(inConv.apply);");
        } else {
            writer.println("    ret = getDelegate().apply(in);");
        }

        if (outArg.getKind() == ClassKind.API) {
            writer.format("    java.util.function.Function<%s, %s> outConv = %s::newInstance;%n", outArg.getName(),
                    genTypeName(outArg.getRaw()), genTypeName(outArg));
            writer.println("    return outConv.apply(ret);");
        } else if (outArg.isVariable()) {
            String typeVar = outArg.getSimpleName();
            writer.format(
                    "    java.util.function.Function<%s, %s> outConv = (java.util.function.Function<%s, %s>) __typeArg_1.wrap;%n",
                    typeVar, typeVar, typeVar, typeVar);
            writer.println("    return outConv.apply(ret);");
        } else {
            writer.println("    return ret;");
        }

        writer.println("  }");
        writer.println();
    }

    @Override
    public boolean test(ClassModel model) {
        return model.isConcrete() && model.isFunction() && model.getMethods().stream()
                .noneMatch(it -> it.getParams().size() == 1 && "apply".equals(it.getName()));
    }
}
