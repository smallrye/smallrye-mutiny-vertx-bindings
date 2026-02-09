package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HashCodeEqualsAndToStringShimTest {

    @SuppressWarnings("unchecked")
    @Test
    void testGeneration() {
        Env env = new Env();
        env.addJavaCode("me.escoffier.test", "MyInterface.java", """
                package me.escoffier.test;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface MyInterface {
                    String foo();
                    static MyInterface create() {
                        return new MyInterface() {
                            public String foo() {
                                return "foo";
                            }
                            public int hashCode() {
                                return 42;
                            }
                            public String toString() {
                                return "hello";
                            }
                            public boolean equals(Object obj) {
                                return true;
                            }
                        };
                    }
                }
                """)
                .addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        Object res = env.invoke(env.getClass("me.escoffier.test.mutiny.MyInterface"), "create");
        assertThat(env.<String> invoke(res, "foo")).isEqualTo("foo");
        assertThat(env.<Integer> invoke(res, "hashCode")).isEqualTo(42);
        assertThat(env.<String> invoke(res, "toString")).isEqualTo("hello");
        assertThat(env.<Boolean> invoke(res, "equals",
                Tuple2.of(Object.class, new Object()))).isEqualTo(false);

    }

    @SuppressWarnings("unchecked")
    @Test
    void testWithInterfaceDefiningToString() {
        Env env = new Env();
        env.addJavaCode("me.escoffier.test", "MyInterface.java", """
                package me.escoffier.test;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface MyInterface {
                    String foo();
                    String toString();
                    static MyInterface create() {
                        return new MyInterface() {
                            public String foo() {
                                return "foo";
                            }
                            public int hashCode() {
                                return 42;
                            }
                            public boolean equals(Object obj) {
                                return true;
                            }
                            public String toString() {
                                return "howdy!";
                            }
                        };
                    }
                }
                """)
                .addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        Object res = env.invoke(env.getClass("me.escoffier.test.mutiny.MyInterface"), "create");
        assertThat(env.<String> invoke(res, "foo")).isEqualTo("foo");
        assertThat(env.<Integer> invoke(res, "hashCode")).isEqualTo(42);
        assertThat(env.<String> invoke(res, "toString")).isEqualTo("howdy!");
        assertThat(env.<Boolean> invoke(res, "equals",
                Tuple2.of(Object.class, new Object()))).isEqualTo(false);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testWithInterfaceDefiningEqualsAndHashCode() {
        Env env = new Env();
        env.addJavaCode("me.escoffier.test", "MyInterface.java", """
                package me.escoffier.test;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface MyInterface {
                    String foo();
                    int hashCode();
                    boolean equals(Object obj);
                    static MyInterface create() {
                        return new MyInterface() {
                            public String foo() {
                                return "foo";
                            }
                            public int hashCode() {
                                return 42;
                            }
                            public boolean equals(Object obj) {
                                return true;
                            }
                            public String toString() {
                                return "howdy!";
                            }
                        };
                    }
                }
                """)
                .addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        Object res = env.invoke(env.getClass("me.escoffier.test.mutiny.MyInterface"), "create");
        assertThat(env.<String> invoke(res, "foo")).isEqualTo("foo");
        assertThat(env.<Integer> invoke(res, "hashCode")).isEqualTo(42);
        assertThat(env.<String> invoke(res, "toString")).isEqualTo("howdy!");
        assertThat(env.<Boolean> invoke(res, "equals",
                Tuple2.of(Object.class, new Object()))).isEqualTo(true); // Delegated even if types mismatch
    }
}
