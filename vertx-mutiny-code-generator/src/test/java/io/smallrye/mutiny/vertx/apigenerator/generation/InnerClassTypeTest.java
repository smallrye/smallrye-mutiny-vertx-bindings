package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

public class InnerClassTypeTest {

    @Test
    public void testMapEntryInField() {
        Env env = new Env();
        env.addJavaCode("org.acme", "UsingMapEntry", """
                package org.acme;

                import java.util.Map;
                import java.util.function.Predicate;
                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface UsingMapEntry {

                    Predicate<Map.Entry<String, Object>> EXCLUDE_ANNOTATION_ENTRIES = entry -> false;
                }
                """);
        env.addModuleGen("org.acme", "UsingMapEntry");
        MutinyGenerator generator = new MutinyGenerator(env.root());
        generator.generate();
        env.compile();
    }
}
