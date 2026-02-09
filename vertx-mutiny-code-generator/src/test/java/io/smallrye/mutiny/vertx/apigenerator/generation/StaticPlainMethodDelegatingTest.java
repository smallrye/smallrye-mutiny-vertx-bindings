package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.palantir.javapoet.MethodSpec;

import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class StaticPlainMethodDelegatingTest {

    @SuppressWarnings("unchecked")
    @Test
    void simple() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyDataObject", """
                        package org.acme;

                        import io.vertx.codegen.annotations.DataObject;

                        @DataObject
                        public class MyDataObject {
                            int value;

                            public MyDataObject setValue(int value) {
                                this.value = value;
                                return this;
                            }

                            public int getValue() {
                                return value;
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
                            static List<String> returnList() {
                              return List.of("a", "b");
                            }
                            static Set<String> returnSet() {
                                return Set.of("a", "b");
                            }
                            static MyDataObject dataObject() {
                                return new MyDataObject().setValue(42);
                            }
                            static int returnPrimitive() {
                                return 42;
                            }
                            static Map<String, MyDataObject> returnMap() {
                                return Map.of("a", new MyDataObject().setValue(42));
                            }
                            static void voidMethod() {

                            }
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = Env.getOutputFor(outputs, "org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).extracting("name").contains("returnList", "returnSet", "returnPrimitive", "returnMap", "dataObject",
                "voidMethod");

        env.compile();
        Class<?> clazz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clazz).isNotNull();
        assertThat(env.invoke(clazz, "returnList")).isEqualTo(List.of("a", "b"));
        assertThat(env.invoke(clazz, "returnSet")).isEqualTo(Set.of("a", "b"));
        assertThat(env.invoke(clazz, "returnPrimitive")).isEqualTo(42);
        Map<String, Object> map = (Map<String, Object>) env.invoke(clazz, "returnMap");
        assertThat(map).hasSize(1).anySatisfy((k, v) -> {
            assertThat(k).isEqualTo("a");
            assertThat(v).extracting("value").isEqualTo(42);
        });
        Object data = env.invoke(clazz, "dataObject");
        assertThat(data).isNotNull().extracting("value").isEqualTo(42);
        assertThat(env.invoke(clazz, "voidMethod")).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void usingTypeParameters() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyInterface<T> {

                            T get();
                            void set(T t);

                            static <T> MyInterface<T> create() {
                              return new MyInterface<T>() {
                                T value;
                                @Override
                                public T get() {
                                  return value;
                                }
                                @Override
                                public void set(T value) {
                                  this.value = value;
                                }
                              };
                            }

                            static MyInterface<String> createString() {
                              return new MyInterface<>() {
                                String value;
                                @Override
                                public String get() {
                                  return value;
                                }
                                @Override
                                public void set(String value) {
                                  this.value = value;
                                }
                              };
                            };
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);

        assertThat(env.compile()).isNotEmpty();

        Class<?> clazz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clazz).isNotNull();

        Object res = env.invoke(clazz, "create");
        var p = new Object();
        env.invoke(res, "set", Tuple2.of(Object.class, p));
        assertThat((Object) env.invoke(res, "get")).isEqualTo(p);

        res = env.invoke(clazz, "createString");
        env.invoke(res, "set", Tuple2.of(Object.class, "foo"));
        assertThat((String) env.invoke(res, "get")).isEqualTo("foo");
    }
}
