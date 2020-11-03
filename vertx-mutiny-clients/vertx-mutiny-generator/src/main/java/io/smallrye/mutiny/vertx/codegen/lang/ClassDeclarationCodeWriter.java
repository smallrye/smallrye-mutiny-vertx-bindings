package io.smallrye.mutiny.vertx.codegen.lang;

import io.smallrye.mutiny.vertx.codegen.MutinyGenerator;
import io.smallrye.mutiny.vertx.codegen.lang.CodeWriter;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.Helper;
import io.vertx.codegen.type.ClassTypeInfo;
import io.vertx.codegen.type.TypeInfo;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class ClassDeclarationCodeWriter implements CodeWriter {

    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        ClassTypeInfo type = model.getType();
        writer.print("public ");
        if (model.isConcrete()) {
            writer.print("class");
        } else {
            writer.print("interface");
        }
        writer.print(" ");
        writer.print(Helper.getSimpleName(model.getIfaceFQCN()));

        if (model.isConcrete() && CodeGenHelper.hasParentClass(model)) {
                writer.print(" extends ");
                writer.print(genTypeName(model.getConcreteSuperType()));
        }

        List<String> interfaces = new ArrayList<>();
        if ("io.vertx.core.buffer.Buffer".equals(type.getName())) {
            interfaces.add("io.vertx.core.shareddata.impl.ClusterSerializable");
        }
        interfaces.addAll(model.getAbstractSuperTypes().stream().map(this::genTypeName).collect(toList()));
        if (model.isHandler()) {
            interfaces.add("io.vertx.core.Handler<" + genTypeName(model.getHandlerArg()) + ">");
        }
        if (model.isIterable()) {
            interfaces.add("java.lang.Iterable<" + genTypeName(model.getIterableArg()) + ">");
        }
        if (model.isIterator()) {
            interfaces.add("java.util.Iterator<" + genTypeName(model.getIteratorArg()) + ">");
        }
        if (model.isFunction()) {
            TypeInfo[] functionArgs = model.getFunctionArgs();
            interfaces.add("java.util.function.Function<" + genTypeName(functionArgs[0]) + ", " + genTypeName(
                    functionArgs[1]) + ">");
        }

        if (!interfaces.isEmpty()) {
            writer.print(
                    interfaces.stream().collect(joining(", ", model.isConcrete() ? " implements " : " extends ", "")));
        }

        writer.println(" {");
        writer.println();
    }
}
