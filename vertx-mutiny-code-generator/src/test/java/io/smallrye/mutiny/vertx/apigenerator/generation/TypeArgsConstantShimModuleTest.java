package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class TypeArgsConstantShimModuleTest {

    @Test
    void testSimple() {
        Env env = new Env();
        env
                .addModuleGen("org.extra", "shimType-args")
                .addJavaCode("org.extra", "MyInterface.java", """
                        package org.extra;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyInterface {
                            void m(String s);
                        }
                        """);
        MutinyGenerator generator = new MutinyGenerator(env.root());
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(generator.generate(), "org.extra.MyInterface");

        assertThat(output.javaFile().toString())
                .contains("public static final TypeArg<MyInterface> __TYPE_ARG")
                .contains("new TypeArg<>(obj -> new MyInterface((org.extra.MyInterface) obj), MyInterface::getDelegate)");

        env.compile();
    }

    @Test
    void testWithGeneric() {
        Env env = new Env();
        env
                .addModuleGen("org.extra", "shimType-args")
                .addJavaCode("org.extra", "MyInterface.java", """
                        package org.extra;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyInterface<T> {
                            T m(String s);
                        }
                        """);
        MutinyGenerator generator = new MutinyGenerator(env.root());
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(generator.generate(), "org.extra.MyInterface");

        assertThat(output.javaFile().toString())
                .contains("public static final TypeArg<MyInterface> __TYPE_ARG")
                .contains("new TypeArg<>(obj -> new MyInterface((org.extra.MyInterface) obj), MyInterface::getDelegate)");

        env.compile();
    }

}
