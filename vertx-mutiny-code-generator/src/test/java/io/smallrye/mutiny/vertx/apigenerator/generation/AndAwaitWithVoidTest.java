package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class AndAwaitWithVoidTest {

    /**
     * Checks that Void becomes void
     */
    @Test
    void testAndAwaitReturningVoid() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;

                        @VertxGen
                        public interface MyInterface {
                            Future<Void> method();
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());
        env.compile();

        assertThat(env.getOutputFor("org.acme.MyInterface").javaFile().toString()).contains("void methodAndAwait()");

    }
}
