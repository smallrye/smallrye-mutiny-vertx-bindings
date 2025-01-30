package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.MutinyGen;
import io.smallrye.mutiny.vertx.TypeArg;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class NonConcreteInterfaceDeclarationTest {

    @Test
    void testWithNonConcrete() {
        Env env = new Env()
                .addJavaCode("me.escoffier.test", "MyInterface.java", """
                        package me.escoffier.test;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete = false)
                        public interface MyInterface {
                            void foo();
                        }
                        """)
                .addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(outputs, "me.escoffier.test.MyInterface");

        assertThat(output.javaFile().typeSpec().kind()).isEqualTo(TypeSpec.Kind.INTERFACE);
        assertThat(output.javaFile().packageName()).isEqualTo("me.escoffier.test.mutiny");
        assertThat(output.javaFile().typeSpec().name()).isEqualTo("MyInterface");

        assertThat(output.javaFile().typeSpec().annotations()).contains(AnnotationSpec.builder(MutinyGen.class)
                .addMember("value", "me.escoffier.test.MyInterface.class").build());

        MethodSpec method = Env.findMethod(output, "getDelegate");
        assertThat(method.returnType()).isEqualTo(ClassName.bestGuess("me.escoffier.test.MyInterface"));

        // We also expect the foo method
        assertThat(Env.findMethod(output, "foo")).isNotNull();

        env.compile();
    }

    @Test
    void testWithNonConcreteWithExtension() throws IOException {
        Env env = new Env()
                .addJavaCode("me.escoffier.test", "MyInterface.java", """
                        package me.escoffier.test;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;

                        @VertxGen(concrete = false)
                        public interface MyInterface extends Handler<String> {
                            void foo();
                        }
                        """)
                .addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(outputs, "me.escoffier.test.MyInterface");

        assertThat(output.javaFile().typeSpec().kind()).isEqualTo(TypeSpec.Kind.INTERFACE);
        assertThat(output.javaFile().packageName()).isEqualTo("me.escoffier.test.mutiny");
        assertThat(output.javaFile().typeSpec().name()).isEqualTo("MyInterface");

        //        assertThat(output.javaFile().typeSpec().superinterfaces())
        //                .contains(ParameterizedTypeName.get(
        //                        ClassName.bestGuess("java.util.function.Consumer"),
        //                        ClassName.bestGuess("java.lang.String")));

        assertThat(output.javaFile().typeSpec().annotations()).contains(AnnotationSpec.builder(MutinyGen.class)
                .addMember("value", "me.escoffier.test.MyInterface.class")
                .build());

        MethodSpec method = Env.findMethod(output, "getDelegate");
        assertThat(method.returnType()).isEqualTo(ClassName.bestGuess("me.escoffier.test.MyInterface"));

        env.compile();
    }

    @Test
    void testWithNonConcreteWithExtensionHavingAVertxGenGeneric() throws IOException {
        Env env = new Env()
                .addJavaCode("me.escoffier.test.ref", "Refed.java", """
                        package me.escoffier.test.ref;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {
                            void bar();
                        }
                        """)
                .addJavaCode("me.escoffier.test", "MyInterface.java", """
                        package me.escoffier.test;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;

                        @VertxGen(concrete = false)
                        public interface MyInterface extends Handler<me.escoffier.test.ref.Refed> {
                            void foo();
                        }
                        """)
                .addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(outputs, "me.escoffier.test.MyInterface");

        assertThat(output.javaFile().typeSpec().kind()).isEqualTo(TypeSpec.Kind.INTERFACE);
        assertThat(output.javaFile().packageName()).isEqualTo("me.escoffier.test.mutiny");
        assertThat(output.javaFile().typeSpec().name()).isEqualTo("MyInterface");

        //        assertThat(output.javaFile().typeSpec().superinterfaces())
        //                .contains(ParameterizedTypeName.get(
        //                        ClassName.bestGuess("java.util.function.Consumer"),
        //                        ClassName.bestGuess("me.escoffier.test.mutiny.ref.Refed")));

        MethodSpec method = Env.findMethod(output, "getDelegate");
        assertThat(method.returnType()).isEqualTo(ClassName.bestGuess("me.escoffier.test.MyInterface"));

        env.compile();
    }

    @Test
    void testWithNonConcreteWithExtensionOfVertxGen() throws IOException {
        Env env = new Env()
                .addJavaCode("me.escoffier.test.ref", "Refed.java", """
                        package me.escoffier.test.ref;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {
                            void bar();
                        }
                        """)
                .addJavaCode("me.escoffier.test", "MyInterface.java", """
                        package me.escoffier.test;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;

                        @VertxGen(concrete = false)
                        public interface MyInterface extends me.escoffier.test.ref.Refed {
                            void foo();
                        }
                        """)
                .addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(outputs, "me.escoffier.test.MyInterface");

        assertThat(output.javaFile().typeSpec().kind()).isEqualTo(TypeSpec.Kind.INTERFACE);
        assertThat(output.javaFile().packageName()).isEqualTo("me.escoffier.test.mutiny");
        assertThat(output.javaFile().typeSpec().name()).isEqualTo("MyInterface");

        //        assertThat(output.javaFile().typeSpec().superinterfaces())
        //                .contains(ClassName.bestGuess("me.escoffier.test.mutiny.ref.Refed"));

        MethodSpec method = Env.findMethod(output, "getDelegate");
        assertThat(method.returnType()).isEqualTo(ClassName.bestGuess("me.escoffier.test.MyInterface"));

        env.compile();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testWithNonConcreteWithInstantiation() {
        Env env = new Env()
                .addJavaCode("me.escoffier.test", "MyInterface.java", """
                        package me.escoffier.test;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;

                        @VertxGen(concrete = false)
                        public interface MyInterface {
                            int foo();
                            Future<String> bar();

                            static MyInterface create() {
                                return new MyInterface() {
                                    public int foo() {
                                        return 3;
                                    }
                                    public Future<String> bar() {
                                        return Future.succeededFuture("bar");
                                    }
                                };
                            }
                        }
                        """)
                .addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(outputs, "me.escoffier.test.MyInterface");

        assertThat(output.javaFile().typeSpec().kind()).isEqualTo(TypeSpec.Kind.INTERFACE);
        assertThat(output.javaFile().packageName()).isEqualTo("me.escoffier.test.mutiny");
        assertThat(output.javaFile().typeSpec().name()).isEqualTo("MyInterface");

        assertThat(output.javaFile().typeSpec().annotations()).contains(AnnotationSpec.builder(MutinyGen.class)
                .addMember("value", "me.escoffier.test.MyInterface.class").build());

        MethodSpec method = Env.findMethod(output, "getDelegate");
        assertThat(method.returnType()).isEqualTo(ClassName.bestGuess("me.escoffier.test.MyInterface"));

        // We also expect the foo method
        assertThat(Env.findMethod(output, "foo")).isNotNull();
        assertThat(Env.findMethod(output, "bar")).isNotNull();

        // newInstance should be added.
        assertThat(Env.findMethod(output, "newInstance", "me.escoffier.test.MyInterface")).isNotNull();

        env.compile();

        // Invoke the `create` method of  "me.escoffier.test.MyInterface" to get an instance to delegate to.
        Class<?> original = env.getClass("me.escoffier.test.MyInterface");
        assertThat(original).isNotNull();
        Object delegate = env.invoke(original, "create");

        Class<?> clz = env.getClass("me.escoffier.test.mutiny.MyInterface");
        assertThat(clz).isNotNull();
        Object instance = env.invoke(clz, "newInstance", Tuple2.of(original, delegate));

        assertThat(instance).isNotNull();
        assertThat((Integer) env.invoke(instance, "foo")).isEqualTo(3);
        assertThat((Object) env.invoke(instance, "bar")).isInstanceOf(Uni.class);

        assertThat((Object) env.invoke(instance, "getDelegate")).isSameAs(delegate);

    }

    @SuppressWarnings("unchecked")
    @Test
    void testWithNonConcreteWithGenericWithInstantiation() {
        Env env = new Env()
                .addJavaCode("me.escoffier.test", "MyInterface.java", """
                        package me.escoffier.test;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;

                        @VertxGen(concrete = false)
                        public interface MyInterface<T> {
                            T foo();
                            Future<T> bar();

                            static <T> MyInterface<T> create(T val) {
                                return new MyInterface<T>() {
                                    public T foo() {
                                        return val;
                                    }
                                    public Future<T> bar() {
                                        return Future.succeededFuture(val);
                                    }
                                };
                            }
                        }
                        """)
                .addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(outputs, "me.escoffier.test.MyInterface");

        assertThat(output.javaFile().typeSpec().kind()).isEqualTo(TypeSpec.Kind.INTERFACE);
        assertThat(output.javaFile().packageName()).isEqualTo("me.escoffier.test.mutiny");
        assertThat(output.javaFile().typeSpec().name()).isEqualTo("MyInterface");

        assertThat(output.javaFile().typeSpec().annotations()).contains(AnnotationSpec.builder(MutinyGen.class)
                .addMember("value", "me.escoffier.test.MyInterface.class").build());

        MethodSpec method = Env.findMethod(output, "getDelegate");
        assertThat(method.returnType().toString()).isEqualTo("me.escoffier.test.MyInterface"); //Erased

        // We also expect the foo method
        assertThat(Env.findMethod(output, "foo")).isNotNull();
        assertThat(Env.findMethod(output, "bar")).isNotNull();

        // newInstance should be added.
        assertThat(Env.findMethod(output, "newInstance", "me.escoffier.test.MyInterface<T>")).isNotNull();

        env.compile();

        // Invoke the `create` method of  "me.escoffier.test.MyInterface" to get an instance to delegate to.
        Class<?> original = env.getClass("me.escoffier.test.MyInterface");
        assertThat(original).isNotNull();
        Object delegate = env.invoke(original, "create", Tuple2.of(Object.class, "Hello"));

        Class<?> clz = env.getClass("me.escoffier.test.mutiny.MyInterface");
        assertThat(clz).isNotNull();

        Object instance = env.invoke(clz, "newInstance", Tuple2.of(original, delegate));
        assertThat(instance).isNotNull();
        assertThat((String) env.invoke(instance, "foo")).isEqualTo("Hello");
        assertThat((Object) env.invoke(instance, "bar")).isInstanceOf(Uni.class);

        assertThat((Object) env.invoke(instance, "getDelegate")).isSameAs(delegate);

        // Use the newInstance method accepting TypeArgs.
        delegate = env.invoke(original, "create", Tuple2.of(Object.class, 42));

        instance = env.invoke(clz, "newInstance", Tuple2.of(original, delegate),
                Tuple2.of(TypeArg.class, TypeArg.of(Integer.class)));
        assertThat(instance).isNotNull();
        assertThat((Integer) env.invoke(instance, "foo")).isEqualTo(42);
        Uni<Integer> uni = env.invoke(instance, "bar");
        assertThat(uni.await().indefinitely()).isEqualTo(42);
        assertThat((Object) env.invoke(instance, "getDelegate")).isSameAs(delegate);
    }

}
