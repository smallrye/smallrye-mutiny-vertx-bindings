package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

/**
 * Checks that Handler<Void> is converted to Runnable.
 */
public class RunnableTest {

    @SuppressWarnings("unchecked")
    @Test
    void testWithPlainMethod() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;

                        @VertxGen
                        public interface MyInterface {
                            void m(Handler<Void> handler);

                            static MyInterface create() {
                                return new MyInterface() {
                                    public void m(Handler<Void> handler) {
                                        handler.handle(null);
                                    }
                                };
                            }
                        }
                        """)
                .addModuleGen("org.acme", "runnable");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();

        AtomicLong ref = new AtomicLong();
        Class<?> shim = env.getClass("org.acme.mutiny.MyInterface");
        var instance = env.invoke(shim, "create");
        Runnable run = new Runnable() {
            @Override
            public void run() {
                ref.set(System.nanoTime());
            }
        };
        env.invoke(instance, "m", Tuple2.of(Runnable.class, run));
        assertThat(ref.get()).isNotZero();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testWithFutureMethod() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;
                        import io.vertx.core.Future;

                        @VertxGen
                        public interface MyInterface {
                            Future<Void> m(Handler<Void> handler);

                            static MyInterface create() {
                                return new MyInterface() {
                                    public Future<Void> m(Handler<Void> handler) {
                                        handler.handle(null);
                                        return Future.succeededFuture();
                                    }
                                };
                            }
                        }
                        """)
                .addModuleGen("org.acme", "runnable");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();

        AtomicLong ref = new AtomicLong();
        Class<?> shim = env.getClass("org.acme.mutiny.MyInterface");
        var instance = env.invoke(shim, "create");
        Runnable run = new Runnable() {
            @Override
            public void run() {
                ref.set(System.nanoTime());
            }
        };
        Uni<Void> uni = env.invoke(instance, "m", Tuple2.of(Runnable.class, run));
        assertThat(uni).isInstanceOf(Uni.class);
        assertThat(ref.get()).isZero(); // No subscription yet
        uni.await().indefinitely();
        assertThat(ref.get()).isNotZero();
    }
}
