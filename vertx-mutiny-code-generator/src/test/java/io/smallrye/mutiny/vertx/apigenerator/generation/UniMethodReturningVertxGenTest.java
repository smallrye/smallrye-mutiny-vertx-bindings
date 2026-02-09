package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.palantir.javapoet.MethodSpec;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class UniMethodReturningVertxGenTest {

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
                                import io.vertx.core.Future;

                                @VertxGen
                                public interface MyInterface {
                                    Future<Refed> returnRefed();

                                    // Test params
                                    Future<Refed> acceptRefed(Refed refed);
                                    Future<Refed> acceptListOfRefed(List<Refed> refed);
                                    Future<Refed> acceptSetOfRefed(Set<Refed> refed);
                                    Future<Refed> acceptMapOfRefed(java.util.Map<String, Refed> refed);
                                    Future<Refed> acceptBasicTypes(int i, String s, List<Integer> list, Set<String> set, Map<String, String> map);

                                    // TODO Test other parameter types (Handler, Supplier, etc.)
                                }
                                """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = Env.getOutputFor(outputs, "org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).extracting("name").contains(
                "returnRefed", "acceptRefed", "acceptListOfRefed", "acceptSetOfRefed", "acceptMapOfRefed", "acceptBasicTypes",
                "returnRefed", "returnRefedAndAwait", "returnRefedAndForget");
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
                                import io.vertx.core.Future;

                                @VertxGen
                                public interface MyInterface<T> {
                                    Future<Refed<T>> returnRefed();

                                    // Test params
                                    Future<Refed<T>> acceptRefed(Refed<T> refed);
                                    Future<Refed<T>> acceptListOfRefed(List<Refed<T>> refed);
                                    Future<Refed<T>> acceptSetOfRefed(Set<Refed<T>> refed);
                                    Future<Refed<T>> acceptMapOfRefed(java.util.Map<String, Refed<T>> refed);
                                    Future<Refed<T>> acceptBasicTypes(int i, String s, List<Integer> list, Set<String> set, Map<String, String> map);

                                    // TODO Test other parameter types (Handler, Supplier, etc.)
                                }
                                """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = Env.getOutputFor(outputs, "org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).extracting("name").contains(
                "returnRefed", "acceptRefed", "acceptListOfRefed", "acceptSetOfRefed", "acceptMapOfRefed", "acceptBasicTypes",
                "returnRefed", "returnRefedAndAwait", "returnRefedAndForget");
    }

    @Test
    void testMethodDefinedInANonConcreteParentReturningAVertxGenObjectWithAVertxGenObjectAsTypeParameter() {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "Pipe", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Pipe<T> {

                            static <T> Pipe<T> create(T content) {
                                return new Pipe<T>() {
                                    public T get() {
                                        return content;
                                    }
                                };
                            }

                            T get();
                        }
                        """)
                .addJavaCode("org.acme", "Message", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Message<T> {

                            static <T> Message<T> create(T body) {
                                return new Message<T>() {
                                    public T body() {
                                        return body;
                                    }
                                };
                            }

                            T body();
                        }
                        """)
                .addJavaCode("org.acme", "MyNonConcreteParent", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete=false)
                        public interface MyNonConcreteParent<T> {
                            Pipe<T> pipe();
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface", """
                                package org.acme;

                                import io.vertx.codegen.annotations.VertxGen;

                                @VertxGen
                                public interface MyInterface<T> extends MyNonConcreteParent<Message<T>> {

                                        static MyInterface<String> create() {
                                            return new MyInterface<String>() {
                                                @Override
                                                public Pipe<Message<String>> pipe() {
                                                    return Pipe.create(Message.create("Hello"));
                                                }
                                            };
                                        }
                                  }
                        """);

        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());
    }
}
