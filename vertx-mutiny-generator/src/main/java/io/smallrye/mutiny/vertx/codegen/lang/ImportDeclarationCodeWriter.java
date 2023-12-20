package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.vertx.TypeArg;
import io.vertx.codegen.annotations.Fluent;

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
        writer.println("import " + TypeArg.class.getName() + ";");
        writer.println("import " + Fluent.class.getName() + ";");
        writer.println("import " + CheckReturnValue.class.getName() + ";");

        Set<String> imported = new HashSet<>();
        for (ClassTypeInfo importedType : model.getImportedTypes()) {
            if (importedType.getKind() != ClassKind.API) {
                if (!importedType.getPackageName().equals("java.lang")) {
                    if (imported.add(importedType.getSimpleName())) {
                        writer.println("import " + importedType + ";");
                    }
                }
            }
        }
        writer.println();
    }

}
