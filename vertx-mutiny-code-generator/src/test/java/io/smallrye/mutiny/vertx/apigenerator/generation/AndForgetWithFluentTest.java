package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AndForgetWithFluentTest {

    @Test
    void testAndForgetReturningThisWhenFluent() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;
                        import io.vertx.codegen.annotations.Fluent;

                        @VertxGen
                        public interface MyInterface {
                            @Fluent
                            Future<MyInterface> method();
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();

        assertThat(env.getOutputFor("org.acme.MyInterface").javaFile().toString()).contains("MyInterface methodAndForget()");

    }

    @Test
    void testAndForgetReturningThisWhenFluentWithTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;
                        import io.vertx.codegen.annotations.Fluent;

                        @VertxGen
                        public interface MyInterface<T> {
                            @Fluent
                            Future<MyInterface<T>> method();
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();

        assertThat(env.getOutputFor("org.acme.MyInterface").javaFile().toString()).contains("MyInterface<T> methodAndForget()");

    }
}
