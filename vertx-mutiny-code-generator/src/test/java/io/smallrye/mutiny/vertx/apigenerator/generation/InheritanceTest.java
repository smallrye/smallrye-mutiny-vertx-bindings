package io.smallrye.mutiny.vertx.apigenerator.generation;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class InheritanceTest {

    @Test
    public void testInheritanceWithTypeParameters() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyMap", """
                        package org.acme;
                        public interface MyMap<K, V>  {
                          V get(Object key);
                          V put(K key, V value);
                        }
                """);
        env.addJavaCode("org.acme", "MyInterface", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface MyInterface<K, V> extends MyMap<K, V> {

                  V get(Object key);
                  V put(K key, V value);
                }
                """);
        env.addModuleGen("org.acme", "MyInterface");
        MutinyGenerator generator = new MutinyGenerator(env.root());
        generator.generate();
        env.compile();

    }

    @Test
    void testWithDeepHierarchy() {
        Env env = new Env();
        env.addJavaCode("org.acme", "I", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface I<T> {
                    void m(T t);
                }
                """)
                .addJavaCode("org.acme", "A", """
                        package org.acme;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete = false)
                        public interface A extends I<String> {
                            void a();
                        }
                        """)
                .addJavaCode("org.acme", "B", """
                        package org.acme;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface B extends A {
                            void b();
                        }
                        """)
                .addJavaCode("org.acme", "C", """
                        package org.acme;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface C extends B {
                            void c();
                        }
                        """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        env.compile();
    }

    @Test
    void shouldOnInheritFromVertxGenInterface() {
        Env env = new Env();
        env.addJavaCode("org.acme", "I", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                public interface I<T> {
                    void m(T t);
                }
                """)
                .addJavaCode("org.acme", "A", """
                        package org.acme;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface A extends I<String> {
                            void a();
                        }
                        """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        env.compile();
    }

    @Test
    void shouldOnInheritFromVertxGenInterfaceWhenUsingNonConcrete() {
        Env env = new Env();
        env.addJavaCode("org.acme", "I", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                public interface I<T> {
                    void m(T t);
                }
                """)
                .addJavaCode("org.acme", "A", """
                        package org.acme;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete = false)
                        public interface A extends I<String> {
                            void a();
                        }
                        """)
                .addJavaCode("org.acme", "B", """
                        package org.acme;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface B extends A {
                            void b();
                        }
                        """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        env.compile();
    }
}
