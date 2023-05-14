package io.smallrye.mutiny.vertx.codegen.lang;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.MutinyHelper;
import io.smallrye.mutiny.vertx.TypeArg;
import io.smallrye.mutiny.vertx.codegen.MutinyGenerator;
import io.vertx.codegen.*;
import io.vertx.codegen.doc.Tag;
import io.vertx.codegen.type.*;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

import javax.lang.model.element.Element;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.smallrye.mutiny.vertx.codegen.MutinyGenerator.ID;
import static io.vertx.codegen.type.ClassKind.*;
import static java.util.stream.Collectors.joining;

public class CodeGenHelper {

    private CodeGenHelper() {
        // avoid direct instantiation
    }

    public static boolean hasParentClass(ClassModel model) {
        TypeInfo concreteSuperType = model.getConcreteSuperType();
        if (concreteSuperType == null) {
            return false;
        }
        if (concreteSuperType.isParameterized()) {
            return !MutinyGenerator.IGNORED_TYPES.contains(concreteSuperType.getRaw().getName());
        } else {
            return !MutinyGenerator.IGNORED_TYPES.contains(concreteSuperType.getName());
        }
    }

    public static MethodKind methodKind(MethodInfo methodInfo) {
        List<ParamInfo> params = methodInfo.getParams();
        int lastParamIndex = params.size() - 1;
        if (lastParamIndex >= 0) {
            TypeInfo lastParamType = params.get(lastParamIndex).getType();
            if (lastParamType.getKind() == ClassKind.HANDLER) {
                TypeInfo typeArg = ((ParameterizedTypeInfo) lastParamType).getArgs().get(0);
                if (typeArg.getKind() == ClassKind.ASYNC_RESULT) {
                    return MethodKind.CALLBACK;
                } else {
                    return MethodKind.HANDLER;
                }
            }
        }
        return MethodKind.OTHER;
    }

    public static String genTypeName(TypeInfo type) {
        return genTypeName(type, false);
    }

    public static String genTranslatedTypeName(TypeInfo type) {
        return genTypeName(type, true);
    }

    protected static boolean isImported(TypeInfo type) {
        switch (type.getKind()) {
            case JSON_OBJECT:
            case JSON_ARRAY:
            case ASYNC_RESULT:
            case HANDLER:
            case LIST:
            case SET:
            case BOXED_PRIMITIVE:
            case STRING:
            case VOID:
            case FUNCTION:
                return true;
            default:
                return false;
        }
    }

    private static String expandInnerTypes(String type) {
        return type.replace('$', '.');
    }

    public static String genTypeName(TypeInfo type, boolean translate) {
        if (!translate && type.isParameterized() && type.getRaw().getName().equals(Uni.class.getName())) {
            ParameterizedTypeInfo parameterizedType = (ParameterizedTypeInfo) type;
            return expandInnerTypes("io.vertx.core.Future<" + genTypeName(parameterizedType.getArg(0), translate) + ">");
        }

        if (type.isParameterized()) {
            ParameterizedTypeInfo pt = (ParameterizedTypeInfo) type;
            if (translate && type.getRaw().getName().equals(Handler.class.getName()) && pt.getArg(0).getName().startsWith(Promise.class.getName())) {
                TypeInfo arg = ((ParameterizedTypeInfo) pt.getArg(0)).getArg(0);
                return expandInnerTypes(Uni.class.getName() + "<" + genTypeName(arg, true) + ">");
            }
            return expandInnerTypes(genTypeName(pt.getRaw(), translate) + pt.getArgs().stream().map(a -> genTypeName(a, translate))
                    .collect(joining(", ", "<", ">")));
        } else {
            if (type.getKind() == ClassKind.API && translate) {
                // TODO Is this still a thing? Future is not API anymore
                if (type.getName().equals(Future.class.getName())) {
                    return expandInnerTypes(Uni.class.getName());
                }
                return expandInnerTypes(type.translateName(ID));
            } else if (translate && type.getName().equals(Future.class.getName())) {
                return expandInnerTypes(Uni.class.getName());
            } else {
                if (isImported(type)) {
                    return expandInnerTypes(type.getSimpleName());
                } else {
                    return expandInnerTypes(type.getName());
                }
            }
        }
    }

    private static boolean isSameType(TypeInfo type, MethodInfo method) {
        ClassKind kind = type.getKind();
        if (type.isDataObjectHolder() || kind.basic || kind.json || kind == ENUM || kind == OTHER || kind == THROWABLE
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

    public static String genConvParam(Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap, TypeInfo type,
                                      MethodInfo method, String expr) {
        if (type.isParameterized() && (type.getRaw().getName().equals(Multi.class.getName()))) {
            ParameterizedTypeInfo parameterizedType = (ParameterizedTypeInfo) type;
            String adapterFunction = "obj -> " + genConvParam(methodTypeArgMap, parameterizedType.getArg(0), method, "obj");
            return "io.smallrye.mutiny.vertx.ReadStreamSubscriber.asReadStream(" + expr + ", " + adapterFunction + ").resume()";
        } else if (type.isParameterized()
                && (type.getRaw().getName().equals(Future.class.getName()))) {
            ParameterizedTypeInfo parameterizedType = (ParameterizedTypeInfo) type;
            TypeInfo arg = parameterizedType.getArg(0);
            if (arg.getKind() == API) {
                return "io.smallrye.mutiny.vertx.UniHelper.toFuture(" + expr + ".map(r -> r.getDelegate()))";
            } else {
                return "io.smallrye.mutiny.vertx.UniHelper.toFuture(" + expr + ")";
            }
        } else if (type.isParameterized() && type.getRaw().getName().equals(Supplier.class.getName())) {
            ParameterizedTypeInfo parameterizedType = (ParameterizedTypeInfo) type;
            if (parameterizedType.getArg(0).getRaw().getName().equals(Future.class.getName())) {
                return "() -> io.smallrye.mutiny.vertx.UniHelper.toFuture(" + expr + ".get())";
            }
        }

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
                    return "new io.smallrye.mutiny.vertx.DelegatingHandler<>(" + expr + ", ar -> ar.map(event -> " + genConvReturn(methodTypeArgMap, resultType, method, "event") + "))";
                } else if (eventType.isParameterized() && eventType.getRaw().getName().equals(Promise.class.getName())) {
                    return "new Handler<" + genTypeName(eventType) + ">() {\n" +
                            "          public void handle(" + genTypeName(eventType) + " event) {\n" +
                            "            " + expr + ".subscribe().with(it -> event.complete(it), failure -> event.fail(failure));\n" +
                            "          }\n" +
                            "      }";
                } else {
                    return "new io.smallrye.mutiny.vertx.DelegatingHandler<>(" + expr + ", event -> " + genConvReturn(methodTypeArgMap, eventType, method, "event") + ")";
                }
            } else if (kind == FUNCTION) {
                TypeInfo argType = parameterizedTypeInfo.getArg(0);
                TypeInfo retType = parameterizedTypeInfo.getArg(1);
                String translatedReturnedType = genTranslatedTypeName(retType);
                if (translatedReturnedType.startsWith("io.smallrye.mutiny.Uni<")) {
                    return generatingAFunctionReturningAFuture(methodTypeArgMap, method, expr, argType, retType);
                } else {
                    return "new java.util.function.Function<" + genTypeName(argType) + "," + retType.getName() + ">() {\n" +
                            "      public " + genTypeName(retType) + " apply(" + genTypeName(argType) + " arg) {\n" +
                            "        " + translatedReturnedType + " ret = " + expr + ".apply("
                            + genConvReturn(methodTypeArgMap, argType, method, "arg") + ");\n" +
                            "        return " + genConvParam(methodTypeArgMap, retType, method, "ret") + ";\n" +
                            "      }\n" +
                            "    }";
                }

            } else if (kind == LIST || kind == SET) {
                return expr + ".stream().map(elt -> " + genConvParam(methodTypeArgMap, parameterizedTypeInfo.getArg(0),
                        method, "elt")
                        + ").collect(java.util.stream.Collectors.to" + type.getRaw().getSimpleName() + "())";
            } else if (kind == MAP) {
                return expr + ".entrySet().stream().collect(java.util.stream.Collectors.toMap(e -> e.getKey(), e -> "
                        + genConvParam(methodTypeArgMap, parameterizedTypeInfo.getArg(1), method, "e.getValue()")
                        + "))";
            } else if (kind == FUTURE) {
                ParameterizedTypeInfo futureType = (ParameterizedTypeInfo) type;
                return expr + ".map(val -> " + genConvParam(methodTypeArgMap, futureType.getArg(0), method, "val") + ")";
            }
        }
        return expr;
    }

    private static String generatingAFunctionReturningAFuture(
            Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap, MethodInfo method, String expr,
            TypeInfo argType, TypeInfo retType) {

        // If the retType if a Future<X> and X is of type API, we need to unwrap the mutiny type to the bare type
        // (call getDelegate())
        boolean appendDelegate = false;
        if (retType.getKind() == FUTURE) {
            ParameterizedTypeInfo ret = (ParameterizedTypeInfo) retType;
            appendDelegate = ret.getArg(0).getKind() == API;
        }

        if (appendDelegate) {
            return "new java.util.function.Function<" + genTypeName(argType) + "," + retType.getName() + ">() {\n" +
                    "      public " + genTypeName(retType) + " apply(" + genTypeName(argType) + " arg) {\n" +
                    "            return io.smallrye.mutiny.vertx.UniHelper.toFuture(\n" +
                    "                 " + expr + ".apply(" + genConvReturn(methodTypeArgMap, argType, method, "arg") + ").map(x -> x.getDelegate())\n" +
                    "            );\n" +
                    "         }\n" +
                    "     }";
        }
        return "new java.util.function.Function<" + genTypeName(argType) + "," + retType.getName() + ">() {\n" +
                "      public " + genTypeName(retType) + " apply(" + genTypeName(argType) + " arg) {\n" +
                "            return io.smallrye.mutiny.vertx.UniHelper.toFuture(\n" +
                "                 " + expr + ".apply(" + genConvReturn(methodTypeArgMap, argType, method, "arg") + ")\n" +
                "            );\n" +
                "         }\n" +
                "     }";

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

    public static void genTypeArg(TypeInfo arg, MethodInfo method, int depth, StringBuilder sb) {
        ClassKind argKind = arg.getKind();
        if (argKind == API) {
            sb.append("new TypeArg<").append(arg.translateName(ID))
                    .append(">(o").append(depth).append(" -> ");
            sb.append(arg.getRaw().translateName(ID)).append(".newInstance((").append(arg.getRaw()).append(")o")
                    .append(depth);
            if (arg instanceof ParameterizedTypeInfo) {
                ParameterizedTypeInfo parameterizedType = (ParameterizedTypeInfo) arg;
                List<TypeInfo> args = parameterizedType.getArgs();
                for (int i = 0; i < args.size(); i++) {
                    sb.append(", ");
                    genTypeArg(args.get(i), method, depth + 1, sb);
                }
            }
            sb.append(")");
            sb.append(", o").append(depth).append(" -> o").append(depth).append(".getDelegate())");
        } else {
            String typeArg = "TypeArg.unknown()";
            if (argKind == OBJECT && arg.isVariable()) {
                String resolved = genTypeArg((TypeVariableInfo) arg, method);
                if (resolved != null) {
                    typeArg = resolved;
                }
            }
            sb.append(typeArg);
        }
    }

    private static String genTypeArg(Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap, TypeInfo arg,
                                     MethodInfo method) {
        Map<TypeInfo, String> typeArgMap = methodTypeArgMap.get(method);
        if (typeArgMap != null) {
            String typeArgRef = typeArgMap.get(arg);
            if (typeArgRef != null) {
                return typeArgRef;
            }
        }
        StringBuilder sb = new StringBuilder();
        genTypeArg(arg, method, 0, sb);
        return sb.toString();
    }

    private static void fail(String m) {
        //        throw new RuntimeException(m);
    }

    public static String writeReturnStatementForApi(Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap,
                                                    TypeInfo type, MethodInfo method, String expr) {
        StringBuilder tmp = new StringBuilder(type.getRaw().translateName(ID));
        tmp.append(".newInstance((");
        tmp.append(type.getRaw());
        tmp.append(")");
        tmp.append(expr);
        if (type.isParameterized()) {
            ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) type;
            for (TypeInfo arg : parameterizedTypeInfo.getArgs()) {
                tmp.append(", ");
                tmp.append(genTypeArg(methodTypeArgMap, arg, method));
            }
        }
        tmp.append(")");
        return tmp.toString();
    }

    public static String genConvReturn(Map<MethodInfo, Map<TypeInfo, String>> methodTypeArgMap, TypeInfo type,
                                       MethodInfo method, String expr) {
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
            return writeReturnStatementForApi(methodTypeArgMap, type, method, expr);
        } else if (type.isParameterized()) {
            ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) type;
            if (kind == HANDLER) {
                TypeInfo abc = parameterizedTypeInfo.getArg(0);
                if (abc.getKind() == ASYNC_RESULT) {
                    TypeInfo tutu = ((ParameterizedTypeInfo) abc).getArg(0);
                    return "new Handler<AsyncResult<" + genTranslatedTypeName(tutu) + ">>() {\n" +
                            "      public void handle(AsyncResult<" + genTranslatedTypeName(tutu) + "> ar) {\n" +
                            "        if (ar.succeeded()) {\n" +
                            "          " + expr + ".handle(io.vertx.core.Future.succeededFuture("
                            + genConvParam(methodTypeArgMap, tutu, method, "ar.result()") + "));\n" +
                            "        } else {\n" +
                            "          " + expr + ".handle(io.vertx.core.Future.failedFuture(ar.cause()));\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }";
                } else {
                    return "new Handler<" + genTranslatedTypeName(abc) + ">() {\n" +
                            "      public void handle(" + genTranslatedTypeName(abc) + " event) {\n" +
                            "          " + expr + ".handle(" + genConvParam(methodTypeArgMap, abc, method, "event")
                            + ");\n" +
                            "      }\n" +
                            "    }";
                }
            } else if (kind == LIST || kind == SET) {
                return expr + ".stream().map(elt -> " + genConvReturn(methodTypeArgMap, parameterizedTypeInfo.getArg(0),
                        method, "elt")
                        + ").collect(java.util.stream.Collectors.to" + type.getRaw().getSimpleName() + "())";
            } else if (kind == MAP) {
                return expr + ".entrySet().stream().collect(Collectors.toMap(_e -> _e.getKey(), _e -> " + genConvReturn(
                        methodTypeArgMap, parameterizedTypeInfo.getArg(1), method, "_e.getValue()") + "))";
            } else if (kind == FUTURE) {
                ParameterizedTypeInfo futureType = (ParameterizedTypeInfo) type;
                return expr + ".map(val -> " + genConvReturn(methodTypeArgMap, futureType.getArg(0), method, "val") + ")";
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
            if (rawType.getKind() == ClassKind.API) {
                Element elt = link.getTargetElement();
                String eltKind = elt.getKind().name();
                String ret = "{@link " + rawType.translateName(ID);
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
        return "{@link " + rawType.getName() + "}";
    }

    public static String renderLinkToHtml(ClassTypeInfo owner, MethodInfo method) {
        if (owner.getKind() == ClassKind.API) {
            String ret = "{@link " + owner.translateName(ID);
            ret += "#" + method.getName();
            if (!method.getParams().isEmpty()) {
                ret += "(" + method.getParams().stream()
                        .map(p -> {
                            TypeInfo type = p.getType();
                            if (type.getKind() == API) {
                                return type.translateName(ID);
                            } else {
                                return type.getSimpleName();
                            }
                        }).collect(joining(",")) + ")";
            }
            ret += "}";
            return ret;
        }
        return "{@link " + owner.getName() + "}";
    }
}
