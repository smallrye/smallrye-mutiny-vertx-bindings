package io.smallrye.mutiny.vertx.apigenerator.collection;

import java.util.ArrayList;
import java.util.List;

public record VertxGenModule(String name, String group, List<String> packageNames) {

    static VertxGenModule merge(VertxGenModule module1, VertxGenModule module2) {
        if (!module1.name().equals(module2.name())) {
            throw new IllegalArgumentException("Cannot merge modules with different names");
        }

        if (!module1.group().equals(module2.group())) {
            throw new IllegalArgumentException("Cannot merge modules with different groups");
        }

        List<String> packages = new ArrayList<>();
        packages.addAll(module1.packageNames());
        packages.addAll(module2.packageNames());
        return new VertxGenModule(module1.name(), module1.group(), packages);
    }

}
