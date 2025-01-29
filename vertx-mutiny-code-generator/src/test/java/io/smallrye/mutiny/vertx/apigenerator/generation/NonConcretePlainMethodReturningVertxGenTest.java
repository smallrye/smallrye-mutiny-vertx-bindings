package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class NonConcretePlainMethodReturningVertxGenTest {

    @Test
    void nonConcreteVertxGen() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {
                            String getValue();
                            static Refed create(String s) {
                                return new Refed() {
                                    public String getValue() {
                                        return s;
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

                        @VertxGen(concrete=false)
                        public interface MyInterface {
                            Refed returnRefed();

                            static MyInterface create(String s) {
                                return new MyInterface() {
                                    public Refed returnRefed() {
                                        return Refed.create(s);
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

        Class<?> delegateClass = env.getClass("org.acme.MyInterface");
        var delegate = env.invoke(delegateClass, "create", Tuple2.of(String.class, "a"));
        assertThat(delegate).isNotNull();

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        var instance = env.invoke(clz, "newInstance", Tuple2.of(delegateClass, delegate));

        assertThat(instance).isNotNull();
        assertThat((Object) env.invoke(instance, "returnRefed"))
                .extracting("value").isEqualTo("a");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create", Tuple2.of(String.class, "b"));
        assertThat(instance).isNotNull();
        assertThat((Object) env.invoke(instance, "returnRefed"))
                .extracting("value").isEqualTo("b");
    }

    @Test
    void nonConcreteVertxGenWithRefedNonConcrete() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete=false)
                        public interface Refed {
                            String getValue();
                            static Refed create(String s) {
                                return new Refed() {
                                    public String getValue() {
                                        return s;
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

                        @VertxGen(concrete=false)
                        public interface MyInterface {
                            Refed returnRefed();

                            static MyInterface create(String s) {
                                return new MyInterface() {
                                    public Refed returnRefed() {
                                        return Refed.create(s);
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

        Class<?> delegateClass = env.getClass("org.acme.MyInterface");
        var delegate = env.invoke(delegateClass, "create", Tuple2.of(String.class, "a"));
        assertThat(delegate).isNotNull();

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        var instance = env.invoke(clz, "newInstance", Tuple2.of(delegateClass, delegate));

        assertThat(instance).isNotNull();
        assertThat((Object) env.invoke(instance, "returnRefed"))
                .extracting("value").isEqualTo("a");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create", Tuple2.of(String.class, "b"));
        assertThat(instance).isNotNull();
        assertThat((Object) env.invoke(instance, "returnRefed"))
                .extracting("value").isEqualTo("b");
    }

    @Test
    void nonConcreteVertxGenWithTypeParamWithRefedNonConcrete() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete=false)
                        public interface Refed<T> {
                            T getValue();
                            static <T> Refed<T> create(T s) {
                                return new Refed<>() {
                                    public T getValue() {
                                        return s;
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

                        @VertxGen(concrete=false)
                        public interface MyInterface<T> {
                            Refed<T> returnRefed();

                            static <T> MyInterface<T> create(T s) {
                                return new MyInterface<>() {
                                    public Refed<T> returnRefed() {
                                        return Refed.create(s);
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

        Class<?> delegateClass = env.getClass("org.acme.MyInterface");
        var delegate = env.invoke(delegateClass, "create", Tuple2.of(Object.class, "a"));
        assertThat(delegate).isNotNull();

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        var instance = env.invoke(clz, "newInstance", Tuple2.of(delegateClass, delegate));

        assertThat(instance).isNotNull();
        assertThat((Object) env.invoke(instance, "returnRefed"))
                .extracting("value").isEqualTo("a");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create", Tuple2.of(Object.class, "b"));
        assertThat(instance).isNotNull();
        assertThat((Object) env.invoke(instance, "returnRefed"))
                .extracting("value").isEqualTo("b");
    }

    @Test
    void nonConcreteVertxGenWithTypeParam() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed<T> {
                            T getValue();
                            static <T> Refed<T> create(T s) {
                                return new Refed<>() {
                                    public T getValue() {
                                        return s;
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

                        @VertxGen(concrete=false)
                        public interface MyInterface<T> {
                            Refed<T> returnRefed();

                            static <T> MyInterface<T> create(T s) {
                                return new MyInterface<>() {
                                    public Refed<T> returnRefed() {
                                        return Refed.create(s);
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

        Class<?> delegateClass = env.getClass("org.acme.MyInterface");
        var delegate = env.invoke(delegateClass, "create", Tuple2.of(Object.class, "a"));
        assertThat(delegate).isNotNull();

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        var instance = env.invoke(clz, "newInstance", Tuple2.of(delegateClass, delegate));

        assertThat(instance).isNotNull();
        assertThat((Object) env.invoke(instance, "returnRefed"))
                .extracting("value").isEqualTo("a");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create", Tuple2.of(Object.class, "b"));
        assertThat(instance).isNotNull();
        assertThat((Object) env.invoke(instance, "returnRefed"))
                .extracting("value").isEqualTo("b");
    }
}
