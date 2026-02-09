package io.smallrye.mutiny.vertx.apigenerator.generation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.VoidType;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimMethod;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimMethodParameter;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

public class NonConcreteInheritanceTest {

    @Test
    void test() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyNonConcreteApi", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen(concrete = false)
                public interface MyNonConcreteApi {
                    void foo();
                }
                """)
                .addJavaCode("org.acme", "MyConcreteApi", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyConcreteApi extends MyNonConcreteApi {
                            void bar();

                            void foo();

                            void foo(String s);
                        }
                        """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();

        env.addOutputs(outputs);
        env.compile();

    }

    @Test
    void testWithTypeParameter() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyNonConcreteApi", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen(concrete = false)
                public interface MyNonConcreteApi<T> {
                    T foo(T s);
                    void toto(int i);
                }
                """)
                .addJavaCode("org.acme", "MyConcreteApi", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyConcreteApi<T> extends MyNonConcreteApi<T> {
                            void bar();

                            T foo(T s);

                            void foo(String s);

                            void toto(int i, long l);
                        }
                        """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();

        env.addOutputs(outputs);
        env.compile();

    }

    @Test
    void testWithDefinedTypeParameter() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyNonConcreteApi", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import java.util.List;

                @VertxGen(concrete = false)
                public interface MyNonConcreteApi<T> {
                    T foo(T s);
                    T baz(T s);
                    void list(List<T> list);
                }
                """)
                .addJavaCode("org.acme", "MyConcreteApi", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import java.util.List;

                        @VertxGen
                        public interface MyConcreteApi extends MyNonConcreteApi<String> {
                            void bar();

                            // foo is not there on purpose
                            // same for list

                            String baz(String s); // Match the type parameter
                        }
                        """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();

        List<ShimMethod> methods = Env.getOutputFor(outputs, "org.acme.MyConcreteApi").shim().getMethods();
        // We should have:
        // void bar()
        // String baz(String s)
        // void list(List<String> list)
        // String foo(String s)
        ShimMethod bar = getMethodByName(methods, "bar");
        ShimMethod baz = getMethodByName(methods, "baz");
        ShimMethod list = getMethodByName(methods, "list");
        ShimMethod foo = getMethodByName(methods, "foo");

        // Check the return type of the methods
        assertThat(bar.getReturnType()).isEqualTo(new VoidType());
        assertThat(baz.getReturnType()).isEqualTo(StaticJavaParser.parseClassOrInterfaceType("java.lang.String"));
        assertThat(list.getReturnType()).isEqualTo(new VoidType());
        assertThat(foo.getReturnType()).isEqualTo(StaticJavaParser.parseClassOrInterfaceType("java.lang.String"));

        // Check the parameters of the methods
        assertThat(bar.getParameters()).isEmpty();
        assertThat(baz.getParameters()).hasSize(1)
                .extracting(ShimMethodParameter::shimType)
                .containsExactly(StaticJavaParser.parseClassOrInterfaceType("java.lang.String"));
        assertThat(list.getParameters()).hasSize(1)
                .extracting(ShimMethodParameter::shimType)
                .containsExactly(StaticJavaParser.parseClassOrInterfaceType("java.util.List<java.lang.String>"));
        assertThat(foo.getParameters()).hasSize(1)
                .extracting(ShimMethodParameter::shimType)
                .containsExactly(StaticJavaParser.parseClassOrInterfaceType("java.lang.String"));

        env.addOutputs(outputs);
        env.compile();

    }

    ShimMethod getMethodByName(List<ShimMethod> methods, String name) {
        return methods.stream().filter(m -> m.getName().equals(name)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Method " + name + " not found"));
    }
}
