package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.TypeInfo;

/**
 * Add methods if the class implements {@code Iterable}, so the {@code iterator} method.
 */
public class IteratableMethodCodeWriter implements ConditionalCodeWriter {

    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        TypeInfo iterableArg = model.getIterableArg();

        writer.println("  @Override");
        writer.printf("  public java.util.Iterator<%s> iterator() {%n", genTypeName(iterableArg));

        if (iterableArg.getKind() == ClassKind.API) {
            writer.format("    java.util.function.Function<%s, %s> conv = %s::newInstance;%n", iterableArg.getName(),
                    genTypeName(iterableArg.getRaw()), genTypeName(iterableArg));
            writer.println("    return new io.smallrye.mutiny.vertx.impl.MappingIterator<>(delegate.iterator(), conv);");
        } else if (iterableArg.isVariable()) {
            String typeVar = iterableArg.getSimpleName();
            writer.format(
                    "    java.util.function.Function<%s, %s> conv = (java.util.function.Function<%s, %s>) __typeArg_0.wrap;%n",
                    typeVar, typeVar, typeVar, typeVar);
            writer.println("    return new io.smallrye.mutiny.vertx.impl.MappingIterator<>(delegate.iterator(), conv);");
        } else {
            writer.println("    return delegate.iterator();");
        }

        writer.println("  }");
        writer.println();
    }

    @Override
    public boolean test(ClassModel model) {
        return model.isConcrete() && model.isIterable()
                && model.getMethods().stream().noneMatch(it -> it.getParams().isEmpty() && "iterator".equals(it.getName()));
    }
}
