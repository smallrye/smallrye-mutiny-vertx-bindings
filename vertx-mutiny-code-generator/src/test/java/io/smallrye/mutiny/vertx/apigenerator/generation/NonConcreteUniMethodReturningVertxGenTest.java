package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class NonConcreteUniMethodReturningVertxGenTest {

    @Test
    void returnFutureOfNonConcreteVertxGenListAndMap() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {
                            String getValue();

                            static Refed create(String value) {
                                return new Refed() {
                                    public String getValue() {
                                        return value;
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
                        import io.vertx.core.Future;

                        @VertxGen(concrete=false)
                        public interface MyInterface {
                            Future<Refed> returnRefed();
                            Future<List<Refed>> returnList();
                            Future<Map<String, Refed>> returnMap();

                            static MyInterface create(String val) {
                                return new MyInterface() {
                                    public Future<Refed> returnRefed() {
                                        return Future.succeededFuture(Refed.create(val));
                                    }
                                    public Future<List<Refed>> returnList() {
                                        return Future.succeededFuture(List.of(Refed.create(val)));
                                    }
                                    public Future<Map<String, Refed>> returnMap() {
                                        return Future.succeededFuture(Map.of("a", Refed.create(val)));
                                    }
                                };
                            }
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        Class<?> original = env.getClass("org.acme.MyInterface");
        assertThat(original).isNotNull();
        Object delegate = env.invoke(original, "create", Tuple2.of(String.class, "hello"));

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        Object instance = env.invoke(clz, "newInstance", Tuple2.of(original, delegate));

        assertThat(instance).isNotNull();
        assertThat(((Uni<Object>) env.invoke(instance, "returnRefed")).await().indefinitely()).extracting("value")
                .isEqualTo("hello");
        assertThat(((Uni<List<Object>>) env.invoke(instance, "returnList")).await().indefinitely()).extracting("value")
                .containsExactly("hello");
        assertThat(((Uni<Map<String, Object>>) env.invoke(instance, "returnMap")).await().indefinitely()).values()
                .extracting("value").containsExactly("hello");

        assertThat((Object) env.invoke(instance, "getDelegate")).isSameAs(delegate);
    }

    @Test
    void returnFutureOfNonConcreteVertxGenListAndMapUsingNonConcreteRefed() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete=false)
                        public interface Refed {
                            String getValue();

                            static Refed create(String value) {
                                return new Refed() {
                                    public String getValue() {
                                        return value;
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
                        import io.vertx.core.Future;

                        @VertxGen(concrete=false)
                        public interface MyInterface {
                            Future<Refed> returnRefed();
                            Future<List<Refed>> returnList();
                            Future<Map<String, Refed>> returnMap();

                            static MyInterface create(String val) {
                                return new MyInterface() {
                                    public Future<Refed> returnRefed() {
                                        return Future.succeededFuture(Refed.create(val));
                                    }
                                    public Future<List<Refed>> returnList() {
                                        return Future.succeededFuture(List.of(Refed.create(val)));
                                    }
                                    public Future<Map<String, Refed>> returnMap() {
                                        return Future.succeededFuture(Map.of("a", Refed.create(val)));
                                    }
                                };
                            }
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        Class<?> original = env.getClass("org.acme.MyInterface");
        assertThat(original).isNotNull();
        Object delegate = env.invoke(original, "create", Tuple2.of(String.class, "hello"));

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        Object instance = env.invoke(clz, "newInstance", Tuple2.of(original, delegate));

        assertThat(instance).isNotNull();
        assertThat(((Uni<Object>) env.invoke(instance, "returnRefed")).await().indefinitely()).extracting("value")
                .isEqualTo("hello");
        assertThat(((Uni<List<Object>>) env.invoke(instance, "returnList")).await().indefinitely()).extracting("value")
                .containsExactly("hello");
        assertThat(((Uni<Map<String, Object>>) env.invoke(instance, "returnMap")).await().indefinitely()).values()
                .extracting("value").containsExactly("hello");

        assertThat((Object) env.invoke(instance, "getDelegate")).isSameAs(delegate);
    }

    @Test
    void returnFutureOfNonConcreteVertxGenListAndMapWithTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed<T> {
                            T getValue();

                            static <T> Refed create(T value) {
                                return new Refed() {
                                    public T getValue() {
                                        return value;
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
                        import io.vertx.core.Future;

                        @VertxGen(concrete=false)
                        public interface MyInterface<T> {
                            Future<Refed<T>> returnRefed();
                            Future<List<Refed<T>>> returnList();
                            Future<Map<String, Refed<T>>> returnMap();

                            static <T> MyInterface<T> create(T val) {
                                return new MyInterface<>() {
                                    public Future<Refed<T>> returnRefed() {
                                        return Future.succeededFuture(Refed.create(val));
                                    }
                                    public Future<List<Refed<T>>> returnList() {
                                        return Future.succeededFuture(List.of(Refed.create(val)));
                                    }
                                    public Future<Map<String, Refed<T>>> returnMap() {
                                        return Future.succeededFuture(Map.of("a", Refed.create(val)));
                                    }
                                };
                            }
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        Class<?> original = env.getClass("org.acme.MyInterface");
        assertThat(original).isNotNull();
        Object delegate = env.invoke(original, "create", Tuple2.of(Object.class, "foo"));

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        Object instance = env.invoke(clz, "newInstance", Tuple2.of(original, delegate));

        assertThat(instance).isNotNull();
        assertThat(((Uni<Object>) env.invoke(instance, "returnRefed")).await().indefinitely()).extracting("value")
                .isEqualTo("foo");
        assertThat(((Uni<List<Object>>) env.invoke(instance, "returnList")).await().indefinitely()).extracting("value")
                .containsExactly("foo");
        assertThat(((Uni<Map<String, Object>>) env.invoke(instance, "returnMap")).await().indefinitely()).values()
                .extracting("value").containsExactly("foo");

        assertThat((Object) env.invoke(instance, "getDelegate")).isSameAs(delegate);
    }

    @Test
    void returnFutureOfNonConcreteVertxGenListAndMapWithTypeParameterAndNonConcreteRefed() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete=false)
                        public interface Refed<T> {
                            T getValue();

                            static <T> Refed create(T value) {
                                return new Refed() {
                                    public T getValue() {
                                        return value;
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
                        import io.vertx.core.Future;

                        @VertxGen(concrete=false)
                        public interface MyInterface<T> {
                            Future<Refed<T>> returnRefed();
                            Future<List<Refed<T>>> returnList();
                            Future<Map<String, Refed<T>>> returnMap();

                            static <T> MyInterface<T> create(T val) {
                                return new MyInterface<>() {
                                    public Future<Refed<T>> returnRefed() {
                                        return Future.succeededFuture(Refed.create(val));
                                    }
                                    public Future<List<Refed<T>>> returnList() {
                                        return Future.succeededFuture(List.of(Refed.create(val)));
                                    }
                                    public Future<Map<String, Refed<T>>> returnMap() {
                                        return Future.succeededFuture(Map.of("a", Refed.create(val)));
                                    }
                                };
                            }
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        Class<?> original = env.getClass("org.acme.MyInterface");
        assertThat(original).isNotNull();
        Object delegate = env.invoke(original, "create", Tuple2.of(Object.class, "foo"));

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        Object instance = env.invoke(clz, "newInstance", Tuple2.of(original, delegate));

        assertThat(instance).isNotNull();
        assertThat(((Uni<Object>) env.invoke(instance, "returnRefed")).await().indefinitely()).extracting("value")
                .isEqualTo("foo");
        assertThat(((Uni<List<Object>>) env.invoke(instance, "returnList")).await().indefinitely()).extracting("value")
                .containsExactly("foo");
        assertThat(((Uni<Map<String, Object>>) env.invoke(instance, "returnMap")).await().indefinitely()).values()
                .extracting("value").containsExactly("foo");

        assertThat((Object) env.invoke(instance, "getDelegate")).isSameAs(delegate);
    }

}
