package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class StaticPlainMethodReturninCollectionOfVertxGenTest {

    @Test
    void produceListAndSetOfVertxGenObject() {
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
                            static List<Refed> returnList() {
                              return List.of(Refed.create("a"), Refed.create("b"));
                            }
                            static Set<Refed> returnSet() {
                                return Set.of(Refed.create("a"), Refed.create("b"));
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
        assertThat((List<?>) env.invoke(clazz, "returnList"))
                .hasSize(2)
                .extracting("name").contains("a", "b");
        assertThat((Set<?>) env.invoke(clazz, "returnSet")).hasSize(2)
                .extracting("name").contains("a", "b");
        ;
    }

    @SuppressWarnings({ "TestFailedLine", "unchecked" })
    @Test
    void produceListAndSetOfVertxGenObjectWithTypeParameter() {
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
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;

                        @VertxGen
                        public interface MyInterface<T> {
                            // Reuse the same type parameter name
                            static <T> List<Refed<T>> list(T a, T b) {
                              return List.of(Refed.create(a), Refed.create(b));
                            }

                            static <I> Set<Refed<I>> set(I a, I b) {
                                return Set.of(Refed.create(a), Refed.create(b));
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
        assertThat((List<?>) env.invoke(clazz, "list", Tuple2.of(Object.class, "a"), Tuple2.of(Object.class, "b")))
                .hasSize(2)
                .extracting("name").contains("a", "b");
        assertThat((Set<?>) env.invoke(clazz, "set", Tuple2.of(Object.class, "a"), Tuple2.of(Object.class, "c")))
                .hasSize(2)
                .extracting("name").contains("a", "c");
    }

}
