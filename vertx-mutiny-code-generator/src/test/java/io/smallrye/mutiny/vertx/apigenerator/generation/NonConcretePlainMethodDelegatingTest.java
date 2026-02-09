package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class NonConcretePlainMethodDelegatingTest {

    @Test
    void nonConcreteVertxGen() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyDataObject", """
                        package org.acme;

                        import io.vertx.codegen.annotations.DataObject;

                        @DataObject
                        public class MyDataObject {

                        }
                        """)
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete = false)
                        public interface MyInterface {
                            String foo();
                            MyDataObject dataObject();

                            public static MyInterface create() {
                                return new MyInterface() {
                                    public String foo() {
                                        return "hello";
                                    }
                                    public MyDataObject dataObject() {
                                        return new MyDataObject();
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

        env.compile();

        // Invoke the `create` method of  "me.escoffier.test.MyInterface" to get an instance to delegate to.
        Class<?> original = env.getClass("org.acme.MyInterface");
        assertThat(original).isNotNull();
        Object delegate = env.invoke(original, "create");

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        Object instance = env.invoke(clz, "newInstance", Tuple2.of(original, delegate));

        assertThat(instance).isNotNull();
        assertThat((String) env.invoke(instance, "foo")).isEqualTo("hello");
        assertThat((Object) env.invoke(instance, "dataObject").getClass().getName()).isEqualTo("org.acme.MyDataObject");

        assertThat((Object) env.invoke(instance, "getDelegate")).isSameAs(delegate);
    }

    @Test
    void nonConcreteVertxGenAndTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyDataObject", """
                        package org.acme;

                        import io.vertx.codegen.annotations.DataObject;

                        @DataObject
                        public class MyDataObject {

                        }
                        """)
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete = false)
                        public interface MyInterface<T> {
                            T foo();
                            MyDataObject dataObject();

                            public static <T> MyInterface create(T t) {
                                return new MyInterface() {
                                    public T foo() {
                                        return t;
                                    }
                                    public MyDataObject dataObject() {
                                        return new MyDataObject();
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

        env.compile();

        // Invoke the `create` method of  "me.escoffier.test.MyInterface" to get an instance to delegate to.
        Class<?> original = env.getClass("org.acme.MyInterface");
        assertThat(original).isNotNull();
        Object delegate = env.invoke(original, "create", Tuple2.of(Object.class, "bye"));

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        Object instance = env.invoke(clz, "newInstance", Tuple2.of(original, delegate));

        assertThat(instance).isNotNull();
        assertThat((String) env.invoke(instance, "foo")).isEqualTo("bye");
        assertThat((Object) env.invoke(instance, "dataObject").getClass().getName()).isEqualTo("org.acme.MyDataObject");

        assertThat((Object) env.invoke(instance, "getDelegate")).isSameAs(delegate);
    }
}
