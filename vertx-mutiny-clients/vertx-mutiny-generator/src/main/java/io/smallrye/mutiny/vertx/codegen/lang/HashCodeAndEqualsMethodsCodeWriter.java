package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.type.ClassTypeInfo;

/**
 * Add the equals and hashCode methods.
 */
public class HashCodeAndEqualsMethodsCodeWriter implements ConditionalCodeWriter {

    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        ClassTypeInfo type = model.getType();
        writer.println("  @Override");
        writer.println("  public boolean equals(Object o) {");
        writer.println("    if (this == o) return true;");
        writer.println("    if (o == null || getClass() != o.getClass()) return false;");
        writer.print("    ");
        writer.print(type.getSimpleName());
        writer.print(" that = (");
        writer.print(type.getSimpleName());
        writer.println(") o;");
        writer.println("    return delegate.equals(that.delegate);");
        writer.println("  }");
        writer.println("  ");

        writer.println("  @Override");
        writer.println("  public int hashCode() {");
        writer.println("    return delegate.hashCode();");
        writer.println("  }");
        writer.println();
    }

    @Override
    public boolean test(ClassModel model) {
        return model.isConcrete();
    }
}
