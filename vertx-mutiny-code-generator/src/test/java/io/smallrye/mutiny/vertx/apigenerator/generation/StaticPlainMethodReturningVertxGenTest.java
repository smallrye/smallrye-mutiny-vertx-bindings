package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StaticPlainMethodReturningVertxGenTest {

    @SuppressWarnings("unchecked")
    @Test
    void produceVertxGenObject() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {
                            String getName();

                            static Refed create(String name) {
                                return new Refed() {
                                    public String getName() {
                                        return name;
                                    }
                                };
                            }
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;

                        @VertxGen
                        public interface MyInterface {
                            static Refed refed(String name) {
                              return Refed.create(name);
                            }
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        env.compile();
        Class<?> clazz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clazz).isNotNull();
        assertThat(env.invoke(clazz, "refed", Tuple2.of(String.class, "Roxanne")))
                .extracting("name").isEqualTo("Roxanne");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    void produceVertxGenObjectWithTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed<T> {
                            T getName();

                            static <T> Refed<T> create(T name) {
                                return new Refed<T>() {
                                    public T getName() {
                                        return name;
                                    }
                                };
                            }
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyInterface<T> {
                            static <I> Refed<I> refed(I name) {
                              return Refed.<I>create(name);
                            }
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        env.compile();
        Class<?> clazz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clazz).isNotNull();
        assertThat(env.invoke(clazz, "refed", Tuple2.of(Object.class, "a")))
                .extracting("name").isEqualTo("a");
    }

}
