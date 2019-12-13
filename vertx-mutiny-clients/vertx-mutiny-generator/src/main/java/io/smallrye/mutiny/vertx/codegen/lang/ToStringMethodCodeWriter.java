package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;
import java.util.List;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.MethodInfo;

/**
 * Add toString if not in the list of method
 */
public class ToStringMethodCodeWriter implements ConditionalCodeWriter {

    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        writer.println("  @Override");
        writer.println("  public String toString() {");
        writer.println("    return delegate.toString();");
        writer.println("  }");
        writer.println();
    }

    @Override
    public boolean test(ClassModel model) {
        List<MethodInfo> methods = model.getMethods();
        return model.isConcrete()
                && methods.stream().noneMatch(it -> it.getParams().isEmpty() && "toString".equals(it.getName()));
    }
}
