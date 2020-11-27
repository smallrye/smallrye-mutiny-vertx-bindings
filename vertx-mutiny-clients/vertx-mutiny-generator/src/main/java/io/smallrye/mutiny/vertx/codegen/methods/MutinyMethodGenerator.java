package io.smallrye.mutiny.vertx.codegen.methods;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.codegen.lang.CodeGenHelper;
import io.vertx.codegen.MethodInfo;
import io.vertx.codegen.ParamInfo;
import io.vertx.codegen.TypeParamInfo;
import io.vertx.codegen.doc.Doc;
import io.vertx.codegen.doc.Token;
import io.vertx.codegen.type.ParameterizedTypeInfo;
import io.vertx.codegen.type.TypeInfo;

import java.io.PrintWriter;

import static io.smallrye.mutiny.vertx.codegen.lang.TypeHelper.isConsumerOfPromise;
import static io.smallrye.mutiny.vertx.codegen.lang.TypeHelper.isHandlerOfPromise;
import static java.util.stream.Collectors.joining;

public class MutinyMethodGenerator {

    protected final PrintWriter writer;

    public MutinyMethodGenerator(PrintWriter writer) {
        this.writer = writer;
    }

    public void generateJavadoc(MutinyMethodDescriptor descriptor) {
        MethodInfo original = descriptor.getOriginalMethod();
        MethodInfo method = descriptor.getMethod();
        Doc doc = original.getDoc();
        boolean deprecated = descriptor.isDeprecated();
        String link = CodeGenHelper
                .renderLinkToHtml(descriptor.getMethod().getOwnerTypes().iterator().next(), descriptor.getMethod());
        if (doc != null) {
            writer.println("  /**");
            if (descriptor.isUniMethod()) {
                Token.toHtml(doc.getTokens(), "   *", CodeGenHelper::renderLinkToHtml, "\n", writer);
                writer.println("   * <p>");
                writer.println("   * Unlike the <em>bare</em> Vert.x variant, this method returns a {@link " + Uni.class
                        .getName() + " Uni}.");
                writer.println("   * Don't forget to <em>subscribe</em> on it to trigger the operation.");
            } else if (descriptor.isAwaitMethod()) {
                writer.println("   * Blocking variant of " + link + ".");
                writer.println("   * <p>");
                writer.println("   * This method waits for the completion of the underlying asynchronous operation.");
                writer.println(
                        "   * If the operation completes successfully, the result is returned, otherwise the failure is thrown (potentially wrapped in a RuntimeException).");
            } else if (descriptor.isForgetMethod()) {
                writer.println("   * Variant of " + link + " that ignores the result of the operation.");
                writer.println("   * <p>");
                writer.println("   * This method subscribes on the result of " + link
                        + ", but discards the outcome (item or failure).");
                writer.println("   * This method is useful to trigger the asynchronous operation from " + link
                        + " but you don't need to compose it with other operations.");
            }

            for (ParamInfo param : method.getParams()) {
                writer.print("   * @param ");
                writer.print(param.getName());
                writer.print(" ");
                if (param.getDescription() != null) {
                    Token.toHtml(param.getDescription().getTokens(), "", CodeGenHelper::renderLinkToHtml, "",
                            writer);
                }
                writer.println();
            }
            if (!method.getReturnType().getName().equalsIgnoreCase("void")) {
                writer.print("   * @return ");
                if (method.getReturnDescription() != null && descriptor.isSimple()) {
                    Token.toHtml(method.getReturnDescription().getTokens(), "",
                            CodeGenHelper::renderLinkToHtml, "",
                            writer);
                } else if (descriptor.isUniMethod()) {
                    writer.print("the {@link " + Uni.class.getName()
                            + " uni} firing the result of the operation when completed, or a failure if the operation failed.");
                } else if (descriptor.isAwaitMethod()) {
                    writer.print(
                            "the " + method.getReturnType().getSimpleName() + " instance produced by the operation.");
                } else if (descriptor.fluent) {
                    writer.print(
                            "the instance of " + method.getReturnType().getSimpleName() + " to chain method calls.");
                }
                writer.println();
            }
            if (deprecated) {
                writer.print("   * @deprecated");
                if (original.getDeprecatedDesc() != null) {
                    writer.print(" " + original.getDeprecatedDesc().getValue());
                }
            }
            writer.println("   */");
        }
    }

    public void generateMethodDeclaration(MutinyMethodDescriptor descriptor) {
        MethodInfo method = descriptor.getMethod();
        if (descriptor.isDeprecated()) {
            writer.println("  @Deprecated");
        }

        if (descriptor.isFluent()) {
            writer.println("  @Fluent");
        }
        if (descriptor.isPrivate()) {
            writer.print("  private ");
        } else {
            writer.print("  public ");
        }
        if (method.isStaticMethod()) {
            writer.print("static ");
        }
        if (method.getTypeParams().size() > 0) {
            writer.print(
                    method.getTypeParams().stream().map(TypeParamInfo::getName).collect(joining(", ", "<", ">")));
            writer.print(" ");
        }
        if (descriptor.isForgetMethod() && !descriptor.fluent) {
            writer.print("void");
        } else if (descriptor.isAwaitMethod() && descriptor.getMethod().getReturnType().isVoid()) {
            writer.print("void");
        } else {
            writer.print(CodeGenHelper.genTranslatedTypeName(method.getReturnType()));
        }
        writer.print(" ");
        writer.print(method.getName());
        writer.print("(");
        writer.print(method.getParams().stream()
                .map(it -> {
                    if (isHandlerOfPromise(it) || isConsumerOfPromise(it)) {
                        ParameterizedTypeInfo type = (ParameterizedTypeInfo) it.getType();
                        TypeInfo promise = type.getArg(0);
                        TypeInfo inner = ((ParameterizedTypeInfo) promise).getArg(0);
                        return Uni.class.getName() + "<" + CodeGenHelper.genTranslatedTypeName(inner) + ">";
                    } else {
                        return CodeGenHelper.genTranslatedTypeName(it.getType()) + " " + it.getName();
                    }
                })
                .collect(joining(", ")));
        writer.print(")");
    }
}
