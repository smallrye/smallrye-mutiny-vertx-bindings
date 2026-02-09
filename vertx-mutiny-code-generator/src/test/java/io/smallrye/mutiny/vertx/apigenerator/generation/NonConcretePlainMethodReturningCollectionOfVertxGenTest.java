package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class NonConcretePlainMethodReturningCollectionOfVertxGenTest {

    @Test
    void withConcrete() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {
                            String getValue();
                            static Refed create(String val) {
                                return new Refed() {
                                    public String getValue() {
                                        return val;
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

                        @VertxGen(concrete = false)
                        public interface MyInterface {
                            List<Refed> returnListOfRefed();
                            Set<Refed> returnSetOfRefed();

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
                            public List<Refed> returnListOfRefed() {
                                return List.of(Refed.create("a"), Refed.create("b"));
                            }

                            @Override
                            public Set<Refed> returnSetOfRefed() {
                                return Set.of(Refed.create("c"), Refed.create("d"));
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
        assertThat((List<?>) env.invoke(instance, "returnListOfRefed")).extracting("value").containsExactly("a", "b");
        assertThat((Set<?>) env.invoke(instance, "returnSetOfRefed")).extracting("value").containsOnly("c", "d");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create");
        assertThat(instance).isNotNull();
        assertThat((List<?>) env.invoke(instance, "returnListOfRefed")).extracting("value").containsExactly("a", "b");
        assertThat((Set<?>) env.invoke(instance, "returnSetOfRefed")).extracting("value").containsOnly("c", "d");

    }

    @Test
    void withRefedBeingNonConcrete() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete=false)
                        public interface Refed {
                            String getValue();
                            static Refed create(String val) {
                                return new Refed() {
                                    public String getValue() {
                                        return val;
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

                        @VertxGen(concrete = false)
                        public interface MyInterface {
                            List<Refed> returnListOfRefed();
                            Set<Refed> returnSetOfRefed();

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
                            public List<Refed> returnListOfRefed() {
                                return List.of(Refed.create("a"), Refed.create("b"));
                            }

                            @Override
                            public Set<Refed> returnSetOfRefed() {
                                return Set.of(Refed.create("c"), Refed.create("d"));
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
        assertThat((List<?>) env.invoke(instance, "returnListOfRefed")).extracting("value").containsExactly("a", "b");
        assertThat((Set<?>) env.invoke(instance, "returnSetOfRefed")).extracting("value").containsOnly("c", "d");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create");
        assertThat(instance).isNotNull();
        assertThat((List<?>) env.invoke(instance, "returnListOfRefed")).extracting("value").containsExactly("a", "b");
        assertThat((Set<?>) env.invoke(instance, "returnSetOfRefed")).extracting("value").containsOnly("c", "d");

    }

    @Test
    void withConcreteAndTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed<T> {
                            T getValue();
                            static <T> Refed<T> create(T val) {
                                return new Refed<>() {
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
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;

                        @VertxGen(concrete = false)
                        public interface MyInterface<T> {
                            List<Refed<T>> returnListOfRefed(T t1, T t2);
                            Set<Refed<T>> returnSetOfRefed(T t1, T t2);

                            static <T> MyInterface<T> create() {
                                return new MyInterfaceImpl<>();
                            }
                        }
                        """)
                .addJavaCode("org.acme", "MyInterfaceImpl", """
                        package org.acme;

                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;

                        public class MyInterfaceImpl<T> implements MyInterface<T> {
                            @Override
                            public List<Refed<T>> returnListOfRefed(T t1, T t2) {
                                return List.of(Refed.create(t1), Refed.create(t2));
                            }

                            @Override
                            public Set<Refed<T>> returnSetOfRefed(T t1, T t2) {
                                return Set.of(Refed.create(t1), Refed.create(t2));
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
        assertThat(
                (List<?>) env.invoke(instance, "returnListOfRefed", Tuple2.of(Object.class, "a"), Tuple2.of(Object.class, "b")))
                .extracting("value").containsExactly("a", "b");
        assertThat(
                (Set<?>) env.invoke(instance, "returnSetOfRefed", Tuple2.of(Object.class, "c"), Tuple2.of(Object.class, "d")))
                .extracting("value").containsOnly("c", "d");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create");
        assertThat(instance).isNotNull();
        assertThat(
                (List<?>) env.invoke(instance, "returnListOfRefed", Tuple2.of(Object.class, "e"), Tuple2.of(Object.class, "f")))
                .extracting("value").containsExactly("e", "f");
        assertThat(
                (Set<?>) env.invoke(instance, "returnSetOfRefed", Tuple2.of(Object.class, "g"), Tuple2.of(Object.class, "h")))
                .extracting("value").containsOnly("g", "h");
    }

    @Test
    void withTypeParameterWithRefedConcrete() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete=false)
                        public interface Refed<T> {
                            T getValue();
                            static <T> Refed<T> create(T val) {
                                return new Refed<>() {
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
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;

                        @VertxGen(concrete = false)
                        public interface MyInterface<T> {
                            List<Refed<T>> returnListOfRefed(T t1, T t2);
                            Set<Refed<T>> returnSetOfRefed(T t1, T t2);

                            static <T> MyInterface<T> create() {
                                return new MyInterfaceImpl<>();
                            }
                        }
                        """)
                .addJavaCode("org.acme", "MyInterfaceImpl", """
                        package org.acme;

                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;

                        public class MyInterfaceImpl<T> implements MyInterface<T> {
                            @Override
                            public List<Refed<T>> returnListOfRefed(T t1, T t2) {
                                return List.of(Refed.create(t1), Refed.create(t2));
                            }

                            @Override
                            public Set<Refed<T>> returnSetOfRefed(T t1, T t2) {
                                return Set.of(Refed.create(t1), Refed.create(t2));
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
        assertThat(
                (List<?>) env.invoke(instance, "returnListOfRefed", Tuple2.of(Object.class, "a"), Tuple2.of(Object.class, "b")))
                .extracting("value").containsExactly("a", "b");
        assertThat(
                (Set<?>) env.invoke(instance, "returnSetOfRefed", Tuple2.of(Object.class, "c"), Tuple2.of(Object.class, "d")))
                .extracting("value").containsOnly("c", "d");

        // Invoke the create method from the shim directly
        clz = env.getClass("org.acme.mutiny.MyInterface");
        instance = env.invoke(clz, "create");
        assertThat(instance).isNotNull();
        assertThat(
                (List<?>) env.invoke(instance, "returnListOfRefed", Tuple2.of(Object.class, "e"), Tuple2.of(Object.class, "f")))
                .extracting("value").containsExactly("e", "f");
        assertThat(
                (Set<?>) env.invoke(instance, "returnSetOfRefed", Tuple2.of(Object.class, "g"), Tuple2.of(Object.class, "h")))
                .extracting("value").containsOnly("g", "h");
    }
}
