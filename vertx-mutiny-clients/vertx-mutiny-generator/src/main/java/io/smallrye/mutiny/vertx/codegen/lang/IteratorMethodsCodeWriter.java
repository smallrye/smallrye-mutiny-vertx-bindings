package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.TypeInfo;

/**
 * Add methods if the class implements {@code Iterator} interface.
 */
public class IteratorMethodsCodeWriter implements ConditionalCodeWriter {

    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        if (model.getMethods().stream().noneMatch(it -> it.getParams().isEmpty() && "hasNext".equals(it.getName()))) {
            writer.println("  @Override");
            writer.println("  public boolean hasNext() {");
            writer.println("    return delegate.hasNext();");
            writer.println("  }");
            writer.println();
        }

        if (model.getMethods().stream().noneMatch(it -> it.getParams().isEmpty() && "next".equals(it.getName()))) {
            TypeInfo iteratorArg = model.getIteratorArg();

            writer.println("  @Override");
            writer.printf("  public %s next() {%n", genTypeName(iteratorArg));

            if (iteratorArg.getKind() == ClassKind.API) {
                writer.format("    return %s.newInstance(delegate.next());%n", genTypeName(iteratorArg));
            } else if (iteratorArg.isVariable()) {
                writer.println("    return __typeArg_0.wrap(delegate.next());");
            } else {
                writer.println("    return delegate.next();");
            }

            writer.println("  }");
            writer.println();
        }
    }

    @Override
    public boolean test(ClassModel model) {
        return model.isConcrete() && model.isIterator();
    }
}
