package io.smallrye.mutiny.vertx.apigenerator.utils;

import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenModule;

public interface ShimConstants {

    static String getPackageName(VertxGenModule module, String packageName) {
        if (packageName.startsWith(module.group())) {
            return module.group() + ".mutiny" + packageName.substring(module.group().length());
        } else {
            throw new IllegalArgumentException("Only expected packages from  the `" + module.group() + "` package");
        }
    }

    static String getClassName(VertxGenModule module, String className) {
        if (className.startsWith(module.group())) {
            return module.group() + ".mutiny" + className.substring(module.group().length());
        } else {
            throw new IllegalArgumentException(
                    "Only expected packages from  the `" + module.group() + "` package, and got " + className);
        }
    }
}
