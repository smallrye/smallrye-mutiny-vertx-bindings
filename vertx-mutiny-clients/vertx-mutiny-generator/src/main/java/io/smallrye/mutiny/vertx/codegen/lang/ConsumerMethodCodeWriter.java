package io.smallrye.mutiny.vertx.codegen.lang;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.TypeInfo;
import io.vertx.core.Handler;

import java.io.PrintWriter;

/**
 * If the class implements {@code Handler<X>}, we added {@code Consumer<X>} and so we need to implement that method.
 */
public class ConsumerMethodCodeWriter implements ConditionalCodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        if (model.isConcrete()) {
            writer.println("  public void accept(" + genTypeName(model.getHandlerArg()) + " item) {");
            writer.println("    handle(item);");
            writer.println("  }");
        } else {
            writer.println("  default public void accept(" + genTypeName(model.getHandlerArg()) + " item) {");
            writer.println("    handle(item);");
            writer.println("  }");
        }
    }

    @Override
    public boolean test(ClassModel classModel) {
        return classModel.isHandler();
    }


}
