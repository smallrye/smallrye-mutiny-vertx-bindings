package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;
import java.util.function.Predicate;

import io.vertx.codegen.ClassModel;

public interface ConditionalCodeWriter extends Predicate<ClassModel>, CodeWriter {

    default Void apply(ClassModel model, PrintWriter writer) {
        if (test(model)) {
            generate(model, writer);
        }
        return null;
    }

}
