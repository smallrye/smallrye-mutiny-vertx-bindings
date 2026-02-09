package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

public class FluentTest {

    // TODO Method returning Vert.x Gen and Collection
    // TODO Method using type parameters

    @Test
    void testPlainDelegatingMethod() {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "Parent", """
                        package org.acme;
                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.codegen.annotations.Fluent;

                         @VertxGen(concrete = false)
                         public interface Parent {
                             @Fluent
                             Parent method();
                         }
                        """)
                .addJavaCode("org.acme", "Intermediate", """
                        package org.acme;
                        import io.vertx.codegen.annotations.VertxGen;

                         @VertxGen
                         public interface Intermediate extends Parent {
                             Intermediate method();
                         }
                        """)
                .addJavaCode("org.acme", "Child", """
                        package org.acme;
                        import io.vertx.codegen.annotations.VertxGen;

                         @VertxGen
                         public interface Child extends Intermediate {
                         }
                        """)
                .addModuleGen("org.acme", "foo");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());
        env.compile();
    }

}
