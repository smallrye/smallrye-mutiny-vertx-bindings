package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class SupplierOfFutureTest {

    @SuppressWarnings("unchecked")
    @Test
    void testSupplierOfFutureOfPlainObject() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyInterface", """
                    package org.acme;

                    import io.vertx.codegen.annotations.VertxGen;
                    import io.vertx.core.Future;
                    import java.util.function.Supplier;
                    @VertxGen
                    public interface MyInterface {
                        Future<String> method(String name, Supplier<Future<String>> supplier);

                       static MyInterface create() {
                            return new MyInterface() {
                                @Override
                                public Future<String> method(String name, Supplier<Future<String>> supplier) {
                                    return supplier.get().map(s -> name + " " + s);
                                }
                            };
                        }
                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        MutinyGenerator.GeneratorOutput output = env.getOutputFor("org.acme.MyInterface");

        env.compile();
        Class<?> shimClass = env.getClass("org.acme.mutiny.MyInterface");
        Supplier<Uni<String>> supplier = () -> Uni.createFrom().item("rocks!");
        Object instance = env.invoke(shimClass, "create");
        var uni = (Uni<String>) env.invoke(instance, "method", Tuple2.of(String.class, "Mutiny"),
                Tuple2.of(Supplier.class, supplier));
        assertThat(uni.await().indefinitely()).isEqualTo("Mutiny rocks!");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSupplierOfFutureOfVertxGenObject() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyVG", """
                package org.acme;
                import io.vertx.codegen.annotations.VertxGen;
                @VertxGen
                public interface MyVG {
                    String name();

                    static MyVG create(String name) {
                        return new MyVG() {
                            @Override
                            public String name() {
                                return name;
                            }
                        };
                    }
                }
                """);
        env.addJavaCode("org.acme", "MyInterface", """
                    package org.acme;

                    import io.vertx.codegen.annotations.VertxGen;
                    import io.vertx.core.Future;
                    import java.util.function.Supplier;
                    @VertxGen
                    public interface MyInterface {
                        Future<String> method(String name, Supplier<Future<MyVG>> supplier);

                       static MyInterface create() {
                            return new MyInterface() {
                                @Override
                                public Future<String> method(String name, Supplier<Future<MyVG>> supplier) {
                                    return supplier.get().map(s -> name + " " + s.name());
                                }
                            };
                        }
                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        MutinyGenerator.GeneratorOutput output = env.getOutputFor("org.acme.MyInterface");

        env.compile();
        Class<?> shimClass = env.getClass("org.acme.mutiny.MyInterface");
        Class<?> shimVGClass = env.getClass("org.acme.mutiny.MyVG");
        var vgInstance = env.invoke(shimVGClass, "create", Tuple2.of(String.class, "rocks!"));
        Supplier<Uni<Object>> supplier = () -> Uni.createFrom().item(vgInstance);
        Object instance = env.invoke(shimClass, "create");
        var uni = (Uni<String>) env.invoke(instance, "method", Tuple2.of(String.class, "Mutiny"),
                Tuple2.of(Supplier.class, supplier));
        assertThat(uni.await().indefinitely()).isEqualTo("Mutiny rocks!");
    }

}
