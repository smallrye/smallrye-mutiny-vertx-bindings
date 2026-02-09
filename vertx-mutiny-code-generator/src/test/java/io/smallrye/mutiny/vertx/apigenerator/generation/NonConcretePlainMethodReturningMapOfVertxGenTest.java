package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class NonConcretePlainMethodReturningMapOfVertxGenTest {

    @Test
    void nonConcreteVertxGen() {
        Env env = new Env().addJavaCode("org.acme", "Refed", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Refed {
                        String getValue();

                        static Refed create(String val) {
                            return new Refed() {
                                @Override
                                public String getValue() {
                                    return val;
                                }
                            };
                        }
                }
                """).addJavaCode("org.acme", "MyInterface", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                @VertxGen(concrete=false)
                public interface MyInterface {
                    Map<String, Refed> returnMapOfRefed();

                    static MyInterface create() {
                        return new MyInterfaceImpl();
                    }
                }
                """)
                .addJavaCode("org.acme", "MyInterfaceImpl", """
                        package org.acme;

                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;

                        public class MyInterfaceImpl implements MyInterface {
                            @Override
                            public Map<String, Refed> returnMapOfRefed() {
                                return Map.of("a", Refed.create("a"), "b", Refed.create("b"));
                            }
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        Class<?> delegateClass = env.getClass("org.acme.MyInterface");
        var delegate = env.invoke(delegateClass, "create");
        assertThat(delegate).isNotNull();
        assertThat(delegate).isInstanceOf(env.getClass("org.acme.MyInterfaceImpl"));

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        var instance = env.invoke(clz, "newInstance", Tuple2.of(delegateClass, delegate));

        assertThat(instance).isNotNull();
        assertThat((Map<String, ?>) env.invoke(instance, "returnMapOfRefed")).values().extracting("value").containsExactly("a",
                "b");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create");
        assertThat(instance).isNotNull();
        assertThat((Map<String, ?>) env.invoke(instance, "returnMapOfRefed")).values().extracting("value").containsExactly("a",
                "b");
    }

    @Test
    void nonConcreteVertxGenWithNonConcreteRefed() {
        Env env = new Env().addJavaCode("org.acme", "Refed", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen(concrete=false)
                public interface Refed {
                        String getValue();

                        static Refed create(String val) {
                            return new Refed() {
                                @Override
                                public String getValue() {
                                    return val;
                                }
                            };
                        }
                }
                """).addJavaCode("org.acme", "MyInterface", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                @VertxGen(concrete=false)
                public interface MyInterface {
                    Map<String, Refed> returnMapOfRefed();

                    static MyInterface create() {
                        return new MyInterfaceImpl();
                    }
                }
                """).addJavaCode("org.acme", "MyInterfaceImpl", """
                package org.acme;

                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                public class MyInterfaceImpl implements MyInterface {
                    @Override
                    public Map<String, Refed> returnMapOfRefed() {
                        return Map.of("a", Refed.create("a"), "b", Refed.create("b"));
                    }
                }
                """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        Class<?> delegateClass = env.getClass("org.acme.MyInterface");
        var delegate = env.invoke(delegateClass, "create");
        assertThat(delegate).isNotNull();
        assertThat(delegate).isInstanceOf(env.getClass("org.acme.MyInterfaceImpl"));

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        var instance = env.invoke(clz, "newInstance", Tuple2.of(delegateClass, delegate));

        assertThat(instance).isNotNull();
        assertThat((Map<String, ?>) env.invoke(instance, "returnMapOfRefed")).values().extracting("value").containsExactly("a",
                "b");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create");
        assertThat(instance).isNotNull();
        assertThat((Map<String, ?>) env.invoke(instance, "returnMapOfRefed")).values().extracting("value").containsExactly("a",
                "b");
    }

    @Test
    void nonConcreteVertxGenWithTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                         package org.acme;

                         import io.vertx.codegen.annotations.VertxGen;

                         @VertxGen
                         public interface Refed<T> {
                             T getValue();

                             static <T> Refed<T> create(T val) {
                                 return new Refed<>() {
                                     @Override
                                    public T getValue() {
                                         return val;
                                    }
                                };
                            }
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import java.util.Map;

                        @VertxGen
                        public interface MyInterface<T> {
                            Map<String, Refed<T>> returnMapOfRefed(T t1, T t2);

                            static <T> MyInterface<T> create() {
                               return new MyInterfaceImpl<>();
                            }
                        }
                        """)
                .addJavaCode("org.acme", "MyInterfaceImpl", """
                        package org.acme;

                        import java.util.Map;

                        public class MyInterfaceImpl<T> implements MyInterface<T> {
                            @Override
                            public Map<String, Refed<T>> returnMapOfRefed(T t1, T t2) {
                                return Map.of("a", Refed.create(t1), "b", Refed.create(t2));
                            }
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        Class<?> delegateClass = env.getClass("org.acme.MyInterface");
        var delegate = env.invoke(delegateClass, "create");
        assertThat(delegate).isNotNull();
        assertThat(delegate).isInstanceOf(env.getClass("org.acme.MyInterfaceImpl"));

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        var instance = env.invoke(clz, "newInstance", Tuple2.of(delegateClass, delegate));

        assertThat(instance).isNotNull();
        assertThat((Map<String, ?>) env.invoke(instance, "returnMapOfRefed", Tuple2.of(Object.class, "a"),
                Tuple2.of(Object.class, "b")))
                .values().extracting("value").containsExactly("a", "b");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create");
        assertThat(instance).isNotNull();
        assertThat((Map<String, ?>) env.invoke(instance, "returnMapOfRefed", Tuple2.of(Object.class, "a"),
                Tuple2.of(Object.class, "b")))
                .values().extracting("value").containsExactly("a", "b");
    }

    @Test
    void nonConcreteVertxGenWithTypeParameterAndNonConcreteRefed() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                         package org.acme;

                         import io.vertx.codegen.annotations.VertxGen;

                         @VertxGen(concrete=false)
                         public interface Refed<T> {
                             T getValue();

                             static <T> Refed<T> create(T val) {
                                 return new Refed<>() {
                                     @Override
                                    public T getValue() {
                                         return val;
                                    }
                                };
                            }
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import java.util.Map;

                        @VertxGen
                        public interface MyInterface<T> {
                            Map<String, Refed<T>> returnMapOfRefed(T t1, T t2);

                            static <T> MyInterface<T> create() {
                               return new MyInterfaceImpl<>();
                            }
                        }
                        """)
                .addJavaCode("org.acme", "MyInterfaceImpl", """
                        package org.acme;

                        import java.util.Map;

                        public class MyInterfaceImpl<T> implements MyInterface<T> {
                            @Override
                            public Map<String, Refed<T>> returnMapOfRefed(T t1, T t2) {
                                return Map.of("a", Refed.create(t1), "b", Refed.create(t2));
                            }
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        Class<?> delegateClass = env.getClass("org.acme.MyInterface");
        var delegate = env.invoke(delegateClass, "create");
        assertThat(delegate).isNotNull();
        assertThat(delegate).isInstanceOf(env.getClass("org.acme.MyInterfaceImpl"));

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        var instance = env.invoke(clz, "newInstance", Tuple2.of(delegateClass, delegate));

        assertThat(instance).isNotNull();
        assertThat((Map<String, ?>) env.invoke(instance, "returnMapOfRefed", Tuple2.of(Object.class, "a"),
                Tuple2.of(Object.class, "b")))
                .values().extracting("value").containsExactly("a", "b");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create");
        assertThat(instance).isNotNull();
        assertThat((Map<String, ?>) env.invoke(instance, "returnMapOfRefed", Tuple2.of(Object.class, "a"),
                Tuple2.of(Object.class, "b")))
                .values().extracting("value").containsExactly("a", "b");
    }
}
