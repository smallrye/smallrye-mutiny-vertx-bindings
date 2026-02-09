package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

/**
 * Some Javadoc form Vert.x uses $ which makes JavaPoet freaks out - they need to be escaped.
 */
public class JavadocUsingDollarTest {

    @Test
    void testJavadocUsingDollar() {
        Env env = new Env();
        env.addJavaCode("me.escoffier.test", "MyInterface.java", """
                package me.escoffier.test;

                import io.vertx.codegen.annotations.VertxGen;

                /**
                * Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut
                * labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco
                * laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in
                * voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat
                * non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
                * @Author: blah
                * @Version: $Rev: 2080 $, $Date: 2010-01-26 18:04:19 +0900 (Tue, 26 Jan 2010) $
                */
                @VertxGen
                public interface MyInterface {
                    String foo();
                }
                """)
                .addModuleGen("me.escoffier.test", "my-module");
        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());
        assertThat(env.getOutputFor("me.escoffier.test.MyInterface").javaFile().toString())
                .contains("Version: $Rev: 2080 $, $Date: 2010-01-26 18:04:19 +0900 (Tue, 26 Jan 2010) $");
    }
}
