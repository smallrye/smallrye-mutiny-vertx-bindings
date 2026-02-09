package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class NonConcreteUniMethodDelegatingTest {

    @Test
    void testMethodReturningFuture() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;
                        import io.vertx.core.Future;

                        @VertxGen(concrete=false)
                        public interface MyInterface {
                            Future<String> returnString();
                            Future<Void> returnVoid();

                            public static MyInterface create() {
                                return new MyInterface() {
                                    public Future<String> returnString() {
                                        return Future.succeededFuture("hello");
                                    }
                                    public Future<Void> returnVoid() {
                                        return Future.succeededFuture();
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

        Class<?> original = env.getClass("org.acme.MyInterface");
        assertThat(original).isNotNull();
        Object delegate = env.invoke(original, "create");

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        Object instance = env.invoke(clz, "newInstance", Tuple2.of(original, delegate));

        assertThat(instance).isNotNull();
        assertThat(((Uni<String>) env.invoke(instance, "returnString")).await().indefinitely()).isEqualTo("hello");
        assertThat(((Uni<Void>) env.invoke(instance, "returnVoid")).await().indefinitely()).isNull();

        assertThat((Object) env.invoke(instance, "getDelegate")).isSameAs(delegate);
    }

    @Test
    void testMethodReturningFutureWithTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;

                        @VertxGen(concrete=false)
                        public interface MyInterface<T> {
                            Future<T> justT();

                            static <T> MyInterface<T> create(T t) {
                                return new MyInterface<T>() {
                                    public Future<T> justT() {
                                        return Future.succeededFuture(t);
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
        Object delegate = env.invoke(original, "create", Tuple2.of(Object.class, 42));

        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        Object instance = env.invoke(clz, "newInstance", Tuple2.of(original, delegate));

        assertThat(instance).isNotNull();
        assertThat(((Uni<Integer>) env.invoke(instance, "justT")).await().indefinitely()).isEqualTo(42);
        assertThat((Object) env.invoke(instance, "getDelegate")).isSameAs(delegate);
    }
}
