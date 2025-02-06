package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.palantir.javapoet.MethodSpec;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class PlainMethodReturningVertxGenTest {

    @Test
    void simple() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {

                        }
                        """)
                .addJavaCode("org.acme", "MyInterface",
                        """
                                package org.acme;

                                import io.vertx.codegen.annotations.VertxGen;
                                import java.util.List;
                                import java.util.Map;
                                import java.util.Set;

                                @VertxGen
                                public interface MyInterface {
                                    Refed returnRefed();

                                    // Test params
                                    Refed acceptRefed(Refed refed);
                                    Refed acceptListOfRefed(List<Refed> refed);
                                    Refed acceptSetOfRefed(Set<Refed> refed);
                                    Refed acceptMapOfRefed(java.util.Map<String, Refed> refed);
                                    Refed acceptBasicTypes(int i, String s, List<Integer> list, Set<String> set, Map<String, String> map);

                                    // TODO Test other parameter types (Handler, Supplier, etc.)
                                }
                                """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = Env.getOutputFor(outputs, "org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).extracting("name").contains("returnRefed", "acceptRefed", "acceptListOfRefed", "acceptSetOfRefed",
                "acceptMapOfRefed", "acceptBasicTypes");
    }

    @Test
    void withTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed<T> {

                        }
                        """)
                .addJavaCode("org.acme", "MyInterface",
                        """
                                package org.acme;

                                import io.vertx.codegen.annotations.VertxGen;
                                import java.util.List;
                                import java.util.Map;
                                import java.util.Set;

                                @VertxGen
                                public interface MyInterface<T> {
                                    Refed<T> returnRefed();

                                    // Test params
                                    Refed<T> acceptRefed(Refed<T> refed);
                                    Refed<T> acceptListOfRefed(List<Refed<T>> refed);
                                    Refed<T> acceptSetOfRefed(Set<Refed<T>> refed);
                                    Refed<T> acceptMapOfRefed(java.util.Map<String, Refed<T>> refed);
                                    Refed<T> acceptBasicTypes(int i, String s, List<Integer> list, Set<String> set, Map<String, String> map);

                                    // TODO Test other parameter types (Handler, Supplier, etc.)
                                }
                                """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = Env.getOutputFor(outputs, "org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).extracting("name").contains("returnRefed", "acceptRefed", "acceptListOfRefed", "acceptSetOfRefed",
                "acceptMapOfRefed", "acceptBasicTypes");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testMethodsReturningNull() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {

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
                            Refed refed();
                            List<Refed> list();
                            Set<Refed> set();
                            Map<String, Refed> map();

                            static Refed staticRefed() {
                                return null;
                            }

                            static List<Refed> staticList() {
                                return null;
                            }

                            static Set<Refed> staticSet() {
                                return null;
                            }

                            static Map<String, Refed> staticMap() {
                                return null;
                            }

                            static MyInterface create() {
                                return new MyInterface() {
                                    @Override
                                    public Refed refed() {
                                        return null;
                                    }

                                    @Override
                                    public List<Refed> list() {
                                        return null;
                                    }

                                    @Override
                                    public Set<Refed> set() {
                                        return null;
                                    }

                                    @Override
                                    public Map<String, Refed> map() {
                                        return null;
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

        Class<?> shim = env.getClass("org.acme.mutiny.MyInterface");
        Object instance = env.invoke(shim, "create");
        assertThat(instance).isNotNull();
        assertThat((Object) env.invoke(instance, "refed")).isNotNull();
        assertThat((Object) env.invoke(instance, "list")).isNull();
        assertThat((Object) env.invoke(instance, "set")).isNull();
        assertThat((Object) env.invoke(instance, "map")).isNull();

        assertThat((Object) env.invoke(shim, "staticRefed")).isNotNull();
        assertThat((Object) env.invoke(shim, "staticList")).isNull();
        assertThat((Object) env.invoke(shim, "staticSet")).isNull();
        assertThat((Object) env.invoke(shim, "staticMap")).isNull();

    }
}
