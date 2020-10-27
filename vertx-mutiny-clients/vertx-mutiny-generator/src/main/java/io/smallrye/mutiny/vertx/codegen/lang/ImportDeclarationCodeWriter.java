package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.smallrye.mutiny.vertx.TypeArg;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.ClassTypeInfo;

public class ImportDeclarationCodeWriter implements CodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        writer.println("import " + Map.class.getName() + ";");
        writer.println("import " + Collectors.class.getName() + ";");
        writer.println("import " + Multi.class.getName() + ";");
        writer.println("import " + Uni.class.getName() + ";");
        writer.println("import " + Consumer.class.getName() + ";");
        writer.println("import " + Subscriber.class.getName() + ";");
        writer.println("import " + Publisher.class.getName() + ";");
        writer.println("import " + TypeArg.class.getName() + ";");

        for (ClassTypeInfo importedType : model.getImportedTypes()) {
            if (importedType.getKind() != ClassKind.API) {
                if (!importedType.getPackageName().equals("java.lang")) {
                    writer.println("import " + importedType + ";");
                }
            }
        }
        writer.println();
    }

}
