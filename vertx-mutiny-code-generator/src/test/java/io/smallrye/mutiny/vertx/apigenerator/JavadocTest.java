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

}
