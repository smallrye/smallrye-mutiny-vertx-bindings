package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class DeprecatedTest {

    @Test
    void checkDeprecation() throws NoSuchMethodException {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyApi", """
                package org.acme;

                import io.vertx.core.Future;
                import io.vertx.codegen.annotations.VertxGen;

                 @VertxGen
                 public interface MyApi {

                     Future<String> foo();

                     @Deprecated
                     Future<String> bar();

                     static MyApi create() {
                        return new MyApi() {
                            @Override
                            public Future<String> foo() {
                                return Future.succeededFuture("foo");
                            }
                            @Override
                            public Future<String> bar() {
                                return Future.succeededFuture("bar");
                            }
                        };
                     }
                 }
                """)
                .addModuleGen("org.acme", "foo");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());
        env.compile();

        Object shim = env.invoke(env.getClass("org.acme.mutiny.MyApi"), "create");
        Method foo = shim.getClass().getMethod("foo");
        Method bar = shim.getClass().getMethod("bar");
        assertThat(foo.getAnnotations())
                .noneMatch(annotation -> annotation.annotationType().equals(Deprecated.class));
        assertThat(bar.getAnnotations())
                .anySatisfy(annotation -> annotation.annotationType().equals(Deprecated.class));
    }
}
