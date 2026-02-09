package io.smallrye.mutiny.vertx.apigenerator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.palantir.javapoet.MethodSpec;

import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class JavadocTest {

    @Test
    void testRewritingLinksFromBareJavadoc() {
        Env env = new Env()
                .addJavaCode("org.acme.foo", "Refed", """
                        package org.acme.foo;
                        import io.vertx.codegen.annotations.VertxGen;
                        @VertxGen
                        public interface Refed {
                            public void foo();
                        }
                        """)
                .addJavaCode("org.acme", "MyClass", """
                        package org.acme;
                        import io.vertx.codegen.annotations.VertxGen;
                        import org.acme.foo.Refed;
                        @VertxGen
                        public interface MyClass {
                            /**
                             * See {@link Refed#foo()}
                             *
                             * @param refed a {@link org.acme.foo.Refed}
                             */
                            public void bar(Refed refed);

                            /**
                             * See {@link #bar(org.acme.foo.Refed)}
                             */
                            public void bar2(Refed refed, int ignored);
                        }
                        """)
                .addModuleGen("org.acme", "org.acme");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();

        MethodSpec bar = env.getOutputFor("org.acme.MyClass").javaFile().typeSpec().methodSpecs()
                .stream().filter(m -> m.name().equals("bar")).findFirst().orElseThrow();
        MethodSpec bar2 = env.getOutputFor("org.acme.MyClass").javaFile().typeSpec().methodSpecs()
                .stream().filter(m -> m.name().equals("bar")).findFirst().orElseThrow();

        assertThat(bar.javadoc().toString()).doesNotContain("org.acme.foo.Refed")
                .contains("org.acme.mutiny.foo.Refed");

        assertThat(bar2.javadoc().toString()).doesNotContain("org.acme.foo.Refed")
                .contains("org.acme.mutiny.foo.Refed");
    }

    @Test
    void ensureNoDoubleReturnWhenAmendingJavadocBecauseOfNullable() {
        Env env = new Env()
                .addModuleGen("org.acme", "test")
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;
                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.codegen.annotations.Nullable;
                        @VertxGen
                        public interface MyInterface {
                            /**
                             * @return a {@link String}
                             */
                            @Nullable String foo();

                             /**
                             * @return a {@link String}, can be null.
                             */
                            @Nullable String bar();

                          /**
                           * Return the first trailer value with the specified name
                           *
                           * @param trailerName  the trailer name
                           * @return the trailer value
                           */
                          @Nullable String getTrailer(String trailerName);
                        }
                        """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());
        env.compile();

        MethodSpec foo = env.getOutputFor("org.acme.MyInterface").javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().equals("foo")).findFirst().orElseThrow();
        assertThat(foo.toString())
                .containsOnlyOnce("return a {@link String}")
                .contains("Can be {@code null}");

        MethodSpec bar = env.getOutputFor("org.acme.MyInterface").javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().equals("bar")).findFirst().orElseThrow();
        assertThat(bar.toString())
                .containsOnlyOnce("return a {@link String}")
                .doesNotContain("Can be {@code null}");

        MethodSpec trailer = env.getOutputFor("org.acme.MyInterface").javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().equals("getTrailer")).findFirst().orElseThrow();

        assertThat(trailer.toString())
                .containsOnlyOnce("@param trailerName")
                .containsOnlyOnce("@return the trailer value. Can be {@code null}.");
    }

}
