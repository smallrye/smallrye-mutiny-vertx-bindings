package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.TypeInfo;

public class ToSubscriberCodeWriter implements ConditionalCodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        TypeInfo itemType = model.getWriteStreamArg();
        writer.format("  private io.smallrye.mutiny.vertx.WriteStreamSubscriber<%s> subscriber;%n",
                genTypeName(itemType));

        writer.println();

        genToSubscriber(itemType, writer);
    }

    private void genToSubscriber(TypeInfo itemType, PrintWriter writer) {
        writer.format("  @CheckReturnValue\n");
        writer.format("  public synchronized io.smallrye.mutiny.vertx.WriteStreamSubscriber<%s> toSubscriber() {%n",
                genTypeName(itemType));
        writer.format("    if (%s == null) {%n", "subscriber");

        if (itemType.getKind() == ClassKind.API) {
            writer.format("      java.util.function.Function<%s, %s> conv = %s::getDelegate;%n", genTypeName(itemType.getRaw()),
                    itemType.getName(), genTypeName(itemType));
            writer.format("      %s = io.smallrye.mutiny.vertx.MutinyHelper.toSubscriber(getDelegate(), conv);%n",
                    "subscriber");
        } else if (itemType.isVariable()) {
            String typeVar = itemType.getSimpleName();
            writer.format(
                    "      java.util.function.Function<%s, %s> conv = (java.util.function.Function<%s, %s>) __typeArg_0.unwrap;%n",
                    typeVar, typeVar, typeVar, typeVar);
            writer.format("      %s = io.smallrye.mutiny.vertx.MutinyHelper.toSubscriber(getDelegate(), conv);%n",
                    "subscriber");
        } else {
            writer.format("      %s = io.smallrye.mutiny.vertx.MutinyHelper.toSubscriber(getDelegate());%n", "subscriber");
        }

        writer.println("    }");
        writer.format("    return %s;%n", "subscriber");
        writer.println("  }");
        writer.println();
    }

    @Override
    public boolean test(ClassModel classModel) {
        return classModel.isConcrete()  && classModel.isWriteStream();
    }
}
