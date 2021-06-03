package io.smallrye.mutiny.vertx.codegen.lang;

import java.io.PrintWriter;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.doc.Doc;
import io.vertx.codegen.doc.Token;
import io.vertx.codegen.type.ClassTypeInfo;

public class ClassJavadocCodeWriter implements CodeWriter {
    @Override
    public void generate(ClassModel model, PrintWriter writer) {
        ClassTypeInfo type = model.getType();
        Doc doc = model.getDoc();
        if (doc != null) {
            writer.println("/**");
            Token.toHtml(doc.getTokens(), " *", CodeGenHelper::renderLinkToHtml, "\n", writer);
            writer.println(" *");
            writer.println(" * <p/>");
            writer.print(" * NOTE: This class has been automatically generated from the {@link ");
            writer.print(type.getName());
            writer.println(" original} non Mutiny-ified interface using Vert.x codegen.");
            writer.println(" */");
            writer.println();
        }
    }
}
