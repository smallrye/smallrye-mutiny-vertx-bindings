package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.palantir.javapoet.MethodSpec;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class UniMethodDelegatingTest {

    @Test
    void testMethodReturningFuture() {
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
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;
                        import io.vertx.core.Future;

                        @VertxGen
                        public interface MyInterface {
                            Future<List<String>> returnList();
                            Future<Set<String>> returnSet();
                            Future<MyDataObject> dataObject();
                            Future<Integer> returnPrimitive();
                            Future<String> returnString();
                            Future<Map<String, MyDataObject>> returnMap();
                            Future<Void> returnVoid();

                            Future<?> returnWildcard();
                            Future<?> allAboutWildcards(List<?> list, Set<List<?>> set);

                            Future<String> returnStringWithParams(List<String> foo, MyDataObject bar);

                            default Future<String> returnVoidAsDefaultMethod() {
                                return Future.succeededFuture("hello");
                            }
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = Env.getOutputFor(outputs, "org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).extracting("name").contains(
                "returnList", "returnSet", "returnPrimitive", "returnMap", "dataObject", "returnVoid", "returnString",
                "returnVoidAsDefaultMethod",
                "returnListAndAwait", "returnListAndForget",
                "returnSetAndAwait", "returnSetAndForget",
                "dataObjectAndAwait", "dataObjectAndForget",
                "returnWildcardAndAwait", "returnWildcardAndForget");

        env.compile();
    }

    @Test
    void withTypeParameter() {
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
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;
                        import io.vertx.core.Future;

                        @VertxGen
                        public interface MyInterface<T> {
                            Future<List<T>> returnList();
                            Future<Set<T>> returnSet();
                            Future<T> justT();
                            Future<MyDataObject> dataObject();
                            Future<Integer> returnPrimitive();
                            Future<Map<String, T>> returnMap();
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = Env.getOutputFor(outputs, "org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).extracting("name").contains(
                "justT", "returnList", "returnSet", "returnPrimitive", "returnMap", "dataObject",
                "dataObjectAndAwait", "dataObjectAndForget",
                "returnPrimitiveAndAwait", "returnPrimitiveAndForget");

        env.compile();
    }

    @Test
    void testThatFutureOutcomeIsNotCached() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;

                        @VertxGen
                        public interface MyInterface {
                            Future<String> returnString();

                            static MyInterface create() {
                                return new MyInterface() {
                                    int i = 0;
                                    public Future<String> returnString() {
                                        return Future.succeededFuture("hello " + i++);
                                    }
                                };
                            }
                        }
                        """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        var shim = env.getClass("org.acme.mutiny.MyInterface");
        var instance = env.invoke(shim, "create");

        Uni<String> uni1 = env.invoke(instance, "returnString");
        Uni<String> uni2 = env.invoke(instance, "returnString");
        Uni<String> uni3 = env.invoke(instance, "returnString");

        assertThat(uni1.await().indefinitely()).isEqualTo("hello 0");
        assertThat(uni2.await().indefinitely()).isEqualTo("hello 1");
        assertThat(uni3.await().indefinitely()).isEqualTo("hello 2");
    }

    @Test
    void testCheckReturnValueAnnotation() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;

                        @VertxGen
                        public interface MyInterface {
                            Future<String> method();
                        }
                        """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = env.getOutputFor("org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).anySatisfy(s -> {
            assertThat(s.name()).isEqualTo("method");
            assertThat(s.annotations()).anyMatch(a -> a.toString().contains("CheckReturnValue"));
        });
        assertThat(specs).anySatisfy(s -> {
            assertThat(s.name()).isEqualTo("methodAndAwait");
            assertThat(s.annotations()).noneMatch(a -> a.toString().contains("CheckReturnValue"));
        });
        assertThat(specs).anySatisfy(s -> {
            assertThat(s.name()).isEqualTo("methodAndForget");
            assertThat(s.annotations()).noneMatch(a -> a.toString().contains("CheckReturnValue"));
        });
    }
}
