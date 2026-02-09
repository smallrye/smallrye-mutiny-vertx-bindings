package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class StaticUniMethodTest {

    @Test
    void testMethodReturningFutures() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;
                        import java.util.List;
                        import java.util.Map;

                        @VertxGen
                        public interface MyInterface {
                            String getValue();

                            static MyInterface create() {
                                return new MyInterface() {
                                    public String getValue() {
                                        return "hello";
                                    }
                                };
                            }

                            static Future<String> future() {
                                return Future.succeededFuture("hello");
                            }

                            static Future<MyInterface> itf() {
                                return Future.succeededFuture(create());
                            }

                            static Future<Void> voidFuture() {
                                return Future.succeededFuture();
                            }

                            static Future<List<MyInterface>> list() {
                                return Future.succeededFuture(List.of(create()));
                            }

                            static Future<Map<String, MyInterface>> map() {
                                return Future.succeededFuture(Map.of("a", create()));
                            }

                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        env.compile();

        Class<?> shim = env.getClass("org.acme.mutiny.MyInterface");
        assertThat(env.invoke(shim, "future")).isInstanceOf(Uni.class)
                .satisfies(uni -> assertThat(((Uni<String>) uni).await().indefinitely()).isEqualTo("hello"));
        assertThat(env.invoke(shim, "voidFuture")).isInstanceOf(Uni.class)
                .satisfies(uni -> assertThat(((Uni<Void>) uni).await().indefinitely()).isNull());
        assertThat(env.invoke(shim, "itf")).isInstanceOf(Uni.class)
                .satisfies(
                        uni -> assertThat(((Uni<Object>) uni).await().indefinitely()).extracting("value").isEqualTo("hello"));
        assertThat(env.invoke(shim, "list")).isInstanceOf(Uni.class)
                .satisfies(uni -> assertThat(((Uni<List<Object>>) uni).await().indefinitely()).extracting("value")
                        .containsExactly("hello"));
        assertThat(env.invoke(shim, "map")).isInstanceOf(Uni.class)
                .satisfies(uni -> assertThat(((Uni<Map<String, Object>>) uni).await().indefinitely()).values()
                        .extracting("value").containsExactly("hello"));
    }

}
