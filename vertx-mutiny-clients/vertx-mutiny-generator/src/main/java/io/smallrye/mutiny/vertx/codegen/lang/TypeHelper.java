package io.smallrye.mutiny.vertx.codegen.lang;

import io.vertx.codegen.ParamInfo;
import io.vertx.codegen.type.ParameterizedTypeInfo;
import io.vertx.codegen.type.TypeInfo;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

import java.util.function.Consumer;

public class TypeHelper {

    public static boolean isHandlerOfPromise(ParamInfo it) {
        boolean parameterized = it.getType().isParameterized();
        if (! parameterized) {
            return false;
        }
        ParameterizedTypeInfo type = (ParameterizedTypeInfo) it.getType();
        if (! type.getRaw().getName().equals(Handler.class.getName())) {
            return false;
        }
        return type.getArg(0).getName().equals(Promise.class.getName());
    }

    public static boolean isConsumerOfPromise(ParamInfo it) {
        return isConsumerOfPromise(it.getType());
    }

    public static boolean isConsumerOfPromise(TypeInfo type) {
        if (! type.isParameterized()) {
            return false;
        }
        ParameterizedTypeInfo parameterized = (ParameterizedTypeInfo) type;
        if (! parameterized.getRaw().getName().equals(Consumer.class.getName())) {
            return false;
        }
        TypeInfo arg = parameterized.getArg(0);
        return arg.isParameterized() && arg.getRaw().getName().equals(Promise.class.getName());
    }
}
