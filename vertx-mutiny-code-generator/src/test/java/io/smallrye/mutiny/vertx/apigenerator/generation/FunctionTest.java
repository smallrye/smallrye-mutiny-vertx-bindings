package io.smallrye.mutiny.vertx.apigenerator.generation;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

/**
 * Verify methods receiving Function as parameter
 */
public class FunctionTest {

    @Test
    void testWithFunctionUsingPlainObjects() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyInterface.java", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import java.util.function.Function;

                @VertxGen
                public interface MyInterface {
                    void method(Function<String, Integer> function);
                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());
        env.compile();
    }

    @Test
    void testWithFunctionPlainToVG() {
        Env env = new Env();
        env.addJavaCode("org.acme", "VG", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface VG {
                    String foo();
                }
                """);
        env.addJavaCode("org.acme", "MyInterface.java", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import java.util.function.Function;

                @VertxGen
                public interface MyInterface {
                    void method(Function<String, VG> function);
                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());
        env.compile();
    }

    @Test
    void testWithFunctionVGToPlain() {
        Env env = new Env();
        env.addJavaCode("org.acme", "VG", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface VG {
                    String foo();
                }
                """);
        env.addJavaCode("org.acme", "MyInterface.java", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import java.util.function.Function;

                @VertxGen
                public interface MyInterface {
                    void method(Function<VG, String> function);
                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());
        env.compile();
    }

    @Test
    void testWithFunctionVGToVG() {
        Env env = new Env();
        env.addJavaCode("org.acme", "VG", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface VG {
                    String foo();
                }
                """);
        env.addJavaCode("org.acme", "VG2", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface VG2 {
                    String foo();
                }
                """);
        env.addJavaCode("org.acme", "MyInterface.java", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import java.util.function.Function;

                @VertxGen
                public interface MyInterface {
                    void method(Function<VG, VG2> function);
                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());
        env.compile();
    }

    @Test
    void testWithFunctionPlainToFutureOfPlain() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyInterface.java", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import java.util.function.Function;
                import io.vertx.core.Future;

                @VertxGen
                public interface MyInterface {
                    void method(Function<String, Future<Integer>> function);
                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
    }

    @Test
    void testWithFunctionPlainToFutureOfVG() {
        Env env = new Env();
        env.addJavaCode("org.acme", "VG", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface VG {
                    String foo();
                }
                """);
        env.addJavaCode("org.acme", "MyInterface.java", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import java.util.function.Function;
                import io.vertx.core.Future;

                @VertxGen
                public interface MyInterface {
                    void method(Function<String, Future<VG>> function);
                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
    }

    @Test
    void testWithFunctionVGToFutureOfPlain() {
        Env env = new Env();
        env.addJavaCode("org.acme", "VG", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface VG {
                    String foo();
                }
                """);
        env.addJavaCode("org.acme", "MyInterface.java", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import java.util.function.Function;
                import io.vertx.core.Future;

                @VertxGen
                public interface MyInterface {
                    void method(Function<VG, Future<Integer>> function);
                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
    }

    @Test
    void testWithFunctionVGToFutureOfVG() {
        Env env = new Env();
        env.addJavaCode("org.acme", "VG", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface VG {
                    String foo();
                }
                """);
        env.addJavaCode("org.acme", "VG2", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface VG2 {
                    String foo();
                }
                """);
        env.addJavaCode("org.acme", "MyInterface.java", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import java.util.function.Function;
                import io.vertx.core.Future;

                @VertxGen
                public interface MyInterface {
                    void method(Function<VG, Future<VG2>> function);
                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
    }
}
