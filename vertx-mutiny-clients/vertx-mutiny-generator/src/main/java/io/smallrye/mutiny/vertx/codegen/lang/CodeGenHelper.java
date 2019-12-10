package io.smallrye.mutiny.vertx.codegen.lang;

import io.smallrye.mutiny.vertx.MutinyHelper;
import io.smallrye.mutiny.vertx.TypeArg;
import io.smallrye.mutiny.vertx.codegen.AbstractMutinyGenerator;
import io.vertx.codegen.*;
import io.vertx.codegen.doc.Tag;
import io.vertx.codegen.type.*;

import javax.lang.model.element.Element;
import java.util.List;

import static io.vertx.codegen.type.ClassKind.*;
import static java.util.stream.Collectors.joining;

public class CodeGenHelper {

    private CodeGenHelper() {
        // avoid direct instantiation
    }

    public static MethodKind methodKind(MethodInfo methodInfo) {
        List<ParamInfo> params = methodInfo.getParams();
        int lastParamIndex = params.size() - 1;
        if (lastParamIndex >= 0) {
            TypeInfo lastParamType = params.get(lastParamIndex).getType();
            if (lastParamType.getKind() == ClassKind.HANDLER) {
                TypeInfo typeArg = ((ParameterizedTypeInfo) lastParamType).getArgs().get(0);
                if (typeArg.getKind() == ClassKind.ASYNC_RESULT) {
                    return MethodKind.FUTURE;
                } else {
                    return MethodKind.HANDLER;
                }
            }
        }
        return MethodKind.OTHER;
    }

    public static String genTypeName(TypeInfo type) {
        if (type.isParameterized()) {
            ParameterizedTypeInfo pt = (ParameterizedTypeInfo) type;
            return genTypeName(pt.getRaw())
                    + pt.getArgs().stream().map(CodeGenHelper::genTypeName).collect(joining(", ", "<", ">"));
        } else if (type.getKind() == ClassKind.API) {
            return type.translateName(AbstractMutinyGenerator.ID);
        } else {
            return type.getSimpleName();
        }
    }

    private static boolean isSameType(TypeInfo type, MethodInfo method) {
        ClassKind kind = type.getKind();
        if (kind.basic || kind.json || kind == DATA_OBJECT || kind == ENUM || kind == OTHER || kind == THROWABLE
                || kind == VOID) {
            return true;
        } else if (kind == OBJECT) {
            if (type.isVariable()) {
                return !isReified((TypeVariableInfo) type, method);
            } else {
                return true;
            }
        } else if (type.isParameterized()) {
            ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) type;
            if (kind == LIST || kind == SET || kind == ASYNC_RESULT) {
                return isSameType(parameterizedTypeInfo.getArg(0), method);
            } else if (kind == MAP) {
                return isSameType(parameterizedTypeInfo.getArg(1), method);
            } else if (kind == HANDLER) {
                return isSameType(parameterizedTypeInfo.getArg(0), method);
            } else if (kind == FUNCTION) {
                return isSameType(parameterizedTypeInfo.getArg(0), method)
                        && isSameType(parameterizedTypeInfo.getArg(1), method);
            }
        }
        return false;
    }

    public static String genConvParam(TypeInfo type, MethodInfo method, String expr) {
        ClassKind kind = type.getKind();
        if (isSameType(type, method)) {
            return expr;
        } else if (kind == OBJECT) {
            if (type.isVariable()) {
                String typeArg = genTypeArg((TypeVariableInfo) type, method);
                if (typeArg != null) {
                    return typeArg + ".<" + type.getName() + ">unwrap(" + expr + ")";
                }
            }
            return expr;
        } else if (kind == API) {
            return expr + ".getDelegate()";
        } else if (kind == CLASS_TYPE) {
            return MutinyHelper.class.getName() + ".unwrap(" + expr + ")";
        } else if (type.isParameterized()) {
            ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) type;
            if (kind == HANDLER) {
                TypeInfo eventType = parameterizedTypeInfo.getArg(0);
                ClassKind eventKind = eventType.getKind();
                if (eventKind == ASYNC_RESULT) {
                    TypeInfo resultType = ((ParameterizedTypeInfo) eventType).getArg(0);
                    return "new Handler<AsyncResult<" + resultType.getName() + ">>() {\n" +
                            "      public void handle(AsyncResult<" + resultType.getName() + "> ar) {\n" +
                            "        if (ar.succeeded()) {\n" +
                            "          " + expr + ".handle(io.vertx.core.Future.succeededFuture("
                            + genConvReturn(resultType, method, "ar.result()") + "));\n" +
                            "        } else {\n" +
                            "          " + expr + ".handle(io.vertx.core.Future.failedFuture(ar.cause()));\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }";
                } else {
                    return "new Handler<" + eventType.getName() + ">() {\n" +
                            "      public void handle(" + eventType.getName() + " event) {\n" +
                            "        " + expr + ".handle(" + genConvReturn(eventType, method, "event") + ");\n" +
                            "      }\n" +
                            "    }";
                }
            } else if (kind == FUNCTION) {
                TypeInfo argType = parameterizedTypeInfo.getArg(0);
                TypeInfo retType = parameterizedTypeInfo.getArg(1);
                return "new java.util.function.Function<" + argType.getName() + "," + retType.getName() + ">() {\n" +
                        "      public " + retType.getName() + " apply(" + argType.getName() + " arg) {\n" +
                        "        " + genTypeName(retType) + " ret = " + expr + ".apply(" + genConvReturn(argType,
                        method, "arg")
                        + ");\n" +
                        "        return " + genConvParam(retType, method, "ret") + ";\n" +
                        "      }\n" +
                        "    }";
            } else if (kind == LIST || kind == SET) {
                return expr + ".stream().map(elt -> " + genConvParam(parameterizedTypeInfo.getArg(0), method, "elt")
                        + ").collect(java.util.stream.Collectors.to" + type.getRaw().getSimpleName() + "())";
            } else if (kind == MAP) {
                return expr + ".entrySet().stream().collect(java.util.stream.Collectors.toMap(e -> e.getKey(), e -> "
                        + genConvParam(parameterizedTypeInfo.getArg(1), method, "e.getValue()") + "))";
            }
        }
        return expr;
    }

    private static boolean isReified(TypeVariableInfo typeVar, MethodInfo method) {
        if (typeVar.isClassParam()) {
            return true;
        } else {
            TypeArgExpression typeArg = method.resolveTypeArg(typeVar);
            return typeArg != null && typeArg.isClassType();
        }
    }

    private static String genTypeArg(TypeVariableInfo typeVar, MethodInfo method) {
        if (typeVar.isClassParam()) {
            return "__typeArg_" + typeVar.getParam().getIndex();
        } else {
            TypeArgExpression typeArg = method.resolveTypeArg(typeVar);
            if (typeArg != null) {
                if (typeArg.isClassType()) {
                    return TypeArg.class.getName() + ".of(" + typeArg.getParam().getName() + ")";
                } else {
                    return typeArg.getParam().getName() + ".__typeArg_" + typeArg.getIndex();
                }
            }
        }
        return null;
    }

    public static String genConvReturn(TypeInfo type, MethodInfo method, String expr) {
        ClassKind kind = type.getKind();
        if (kind == OBJECT) {
            if (type.isVariable()) {
                String typeArg = genTypeArg((TypeVariableInfo) type, method);
                if (typeArg != null) {
                    return "(" + type.getName() + ")" + typeArg + ".wrap(" + expr + ")";
                }
            }
            return "(" + type.getSimpleName() + ") " + expr;
        } else if (isSameType(type, method)) {
            return expr;
        } else if (kind == API) {
            StringBuilder tmp = new StringBuilder(type.getRaw().translateName(AbstractMutinyGenerator.ID));
            tmp.append(".newInstance(");
            tmp.append(expr);
            if (type.isParameterized()) {
                ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) type;
                for (TypeInfo arg : parameterizedTypeInfo.getArgs()) {
                    tmp.append(", ");
                    ClassKind argKind = arg.getKind();
                    if (argKind == API) {
                        tmp.append("(io.smallrye.mutiny.vertx.TypeArg)").append(arg.getRaw().translateName(
                                AbstractMutinyGenerator.ID))
                                .append(".__TYPE_ARG");
                    } else {
                        String typeArg = TypeArg.class.getName() + ".unknown()";
                        if (argKind == OBJECT && arg.isVariable()) {
                            String resolved = genTypeArg((TypeVariableInfo) arg, method);
                            if (resolved != null) {
                                typeArg = resolved;
                            }
                        }
                        tmp.append(typeArg);
                    }
                }
            }
            tmp.append(")");
            return tmp.toString();
        } else if (type.isParameterized()) {
            ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) type;
            if (kind == HANDLER) {
                TypeInfo abc = parameterizedTypeInfo.getArg(0);
                if (abc.getKind() == ASYNC_RESULT) {
                    TypeInfo tutu = ((ParameterizedTypeInfo) abc).getArg(0);
                    return "new Handler<AsyncResult<" + genTypeName(tutu) + ">>() {\n" +
                            "      public void handle(AsyncResult<" + genTypeName(tutu) + "> ar) {\n" +
                            "        if (ar.succeeded()) {\n" +
                            "          " + expr + ".handle(io.vertx.core.Future.succeededFuture("
                            + genConvParam(tutu, method, "ar.result()") + "));\n" +
                            "        } else {\n" +
                            "          " + expr + ".handle(io.vertx.core.Future.failedFuture(ar.cause()));\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }";
                } else {
                    return "new Handler<" + genTypeName(abc) + ">() {\n" +
                            "      public void handle(" + genTypeName(abc) + " event) {\n" +
                            "          " + expr + ".handle(" + genConvParam(abc, method, "event") + ");\n" +
                            "      }\n" +
                            "    }";
                }
            } else if (kind == LIST || kind == SET) {
                return expr + ".stream().map(elt -> " + genConvReturn(parameterizedTypeInfo.getArg(0), method, "elt")
                        + ").collect(java.util.stream.Collectors.to" + type.getRaw().getSimpleName() + "())";
            }
        }
        return expr;
    }

    static String genOptTypeParamsDecl(ClassTypeInfo type, String deflt) {
        if (type.getParams().size() > 0) {
            return type.getParams().stream().map(TypeParamInfo::getName).collect(joining(",", "<", ">"));
        } else {
            return deflt;
        }
    }

    public static String renderLinkToHtml(Tag.Link link) {
        ClassTypeInfo rawType = link.getTargetType().getRaw();
        if (rawType.getModule() != null) {
            String label = link.getLabel().trim();
            if (rawType.getKind() == DATA_OBJECT) {
                return "{@link " + rawType.getName() + "}";
            } else {
                if (rawType.getKind() == ClassKind.API) {
                    Element elt = link.getTargetElement();
                    String eltKind = elt.getKind().name();
                    String ret = "{@link " + rawType.translateName(AbstractMutinyGenerator.ID);
                    if ("METHOD".equals(eltKind)) {
                        /* todo find a way for translating the complete signature */
                        ret += "#" + elt.getSimpleName().toString();
                    }
                    if (label.length() > 0) {
                        ret += " " + label;
                    }
                    ret += "}";
                    return ret;
                }
            }
        }
        return "{@link " + rawType.getName() + "}";
    }
}
