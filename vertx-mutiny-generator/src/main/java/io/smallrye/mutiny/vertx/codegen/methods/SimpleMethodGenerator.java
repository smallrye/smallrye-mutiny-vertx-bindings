package io.smallrye.mutiny.vertx.codegen.methods;

import io.smallrye.mutiny.vertx.ReadStreamSubscriber;
import io.smallrye.mutiny.vertx.codegen.lang.CodeGenHelper;
import io.vertx.codegen.ClassModel;
import io.vertx.codegen.Helper;
import io.vertx.codegen.MethodInfo;
import io.vertx.codegen.ParamInfo;
import io.vertx.codegen.type.ParameterizedTypeInfo;
import io.vertx.codegen.type.PrimitiveTypeInfo;
import io.vertx.codegen.type.TypeInfo;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static io.vertx.codegen.type.ClassKind.PRIMITIVE;

public class SimpleMethodGenerator extends MutinyMethodGenerator {

    private final List<String> cacheDecls;
    private final Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap;

    public SimpleMethodGenerator(PrintWriter writer, List<String> cacheDecls,
            Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap) {
        super(writer);
        this.cacheDecls = cacheDecls;
        this.methodTypeArgMap = methodTypeArgMap;
    }

    public void generateDeclaration(MethodInfo method) {
        // Use other as we don't change the signature and the name
        MutinyMethodDescriptor consumerMethod = computeMethodInfoOther(method);
        generateJavadoc(consumerMethod);
        generateMethodDeclaration(consumerMethod);
        writer.println(";");
        writer.println();
    }

    public void generate(ClassModel model, MethodInfo method) {
        MutinyMethodDescriptor simpleMethod = computeMethodInfo(method);
        generateJavadoc(simpleMethod);
        generateMethodDeclaration(simpleMethod);
        generateBody(model, simpleMethod);

        writer.println();
    }

    private void generateBody(ClassModel model, MutinyMethodDescriptor descriptor) {
        writer.println(" { ");
        MethodInfo original = descriptor.getOriginalMethod();
        if (descriptor.isFluent()) {
            writer.print("    ");
            writer.print(genInvokeDelegate(model, original));
            writer.println(";");
            if (original.getReturnType().isVariable()) {
                writer.print("    return (");
                writer.print(original.getReturnType().getName());
                writer.println(") this;");
            } else {
                writer.println("    return this;");
            }
        } else if (original.getReturnType().getName().equals("void")) {
            writer.print("    ");
            writer.print(genInvokeDelegate(model, original));
            writer.println(";");
        } else {
            if (original.isCacheReturn()) {
                writer.print("    if (cached_");
                writer.print(cacheDecls.size());
                writer.println(" != null) {");

                writer.print("      return cached_");
                writer.print(cacheDecls.size());
                writer.println(";");
                writer.println("    }");
            }
            String cachedType;
            TypeInfo returnType = original.getReturnType();
            if (original.getReturnType().getKind() == PRIMITIVE) {
                cachedType = ((PrimitiveTypeInfo) returnType).getBoxed().getName();
            } else {
                cachedType = CodeGenHelper.genTranslatedTypeName(returnType);
            }
            writer.print("    ");
            writer.print(CodeGenHelper.genTranslatedTypeName(returnType));
            writer.print(" ret = ");
            writer.print(CodeGenHelper
                    .genConvReturn(methodTypeArgMap, returnType, original,
                            genInvokeDelegate(model, original)));
            writer.println(";");
            if (original.isCacheReturn()) {
                writer.print("    cached_");
                writer.print(cacheDecls.size());
                writer.println(" = ret;");
                cacheDecls.add("private" + (original.isStaticMethod() ? " static" : "") + " " + cachedType + " cached_"
                        + cacheDecls.size());
            }
            writer.println("    return ret;");
        }
        writer.println("  }");
    }

    private MutinyMethodDescriptor computeMethodInfo(MethodInfo method) {
        return new MutinyMethodDescriptor(method.copy().setName("__" + method.getName()), method,
                MutinyMethodDescriptor.MutinyKind.SIMPLE, method.isFluent(), true);
    }

    private MutinyMethodDescriptor computeMethodInfoOther(MethodInfo method) {
        return new MutinyMethodDescriptor(method.copy(), method,
                MutinyMethodDescriptor.MutinyKind.SIMPLE, method.isFluent());
    }

    public String genInvokeDelegate(ClassModel model, MethodInfo method) {
        StringBuilder ret;
        if (method.isStaticMethod()) {
            ret = new StringBuilder(Helper.getNonGenericType(model.getIfaceFQCN()));
        } else {
            ret = new StringBuilder("delegate");
        }
        ret.append(".").append(method.getName()).append("(");
        int index = 0;
        for (ParamInfo param : method.getParams()) {
            if (index > 0) {
                ret.append(", ");
            }
            TypeInfo type = param.getType();
            if (type.isParameterized() && (type.getRaw().getName().equals("java.util.concurrent.Flow$Publisher"))) {
                String adapterFunction;
                ParameterizedTypeInfo parameterizedType = (ParameterizedTypeInfo) type;
                if (parameterizedType.getArg(0).isVariable()) {
                    adapterFunction = "java.util.function.Function.identity()";
                } else {
                    adapterFunction =
                            "obj -> (" + parameterizedType.getArg(0).getRaw().getName() + ")obj.getDelegate()";
                }
                ret.append(ReadStreamSubscriber.class.getName()).append(".asReadStream(")
                        .append(param.getName())
                        .append(",")
                        .append(adapterFunction).append(").resume()");
            } else {
                ret.append(CodeGenHelper.genConvParam(methodTypeArgMap, type, method, param.getName()));
            }
            index = index + 1;
        }
        ret.append(")");
        return ret.toString();
    }

    public void generateOther(ClassModel model, MethodInfo method) {
        if (isStreamMethod(model, method)) {
            genStreamMethod(model, writer);
            return;
        }
        MutinyMethodDescriptor simpleMethod = computeMethodInfoOther(method);
        generateJavadoc(simpleMethod);
        generateMethodDeclaration(simpleMethod);
        generateBody(model, simpleMethod);

        writer.println();
    }

    private boolean isStreamMethod(ClassModel model, MethodInfo method) {
        return model.isIterable() && method.getName().equals("stream") && method.getParams().isEmpty() && method.getReturnType().getRaw().getName().equals("java.util.stream.Stream");
    }

    private void genStreamMethod(ClassModel model, PrintWriter writer) {
        writer.printf("  public java.util.stream.Stream<%s> stream() {%n", CodeGenHelper.genTranslatedTypeName(model.getIterableArg()));
        writer.println("    return java.util.stream.StreamSupport.stream(spliterator(), false);");
        writer.println("  }");
        writer.println();
    }
}
