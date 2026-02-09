package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.palantir.javapoet.MethodSpec;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

/**
 * Check the support of Vert.x Gen interface implementing Iterable:
 * - The items can be Vert.x gen or not.
 * - The generated class should implement Iterable, with the item being shim or not
 */
public class IterableTest {

    @Test
    void testIterableOfVertxGen() {
        Env env = new Env();
        env.addJavaCode("org.acme", "Refed", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Refed {
                }
                """);
        env.addJavaCode("org.acme", "MyInterface", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface MyInterface extends Iterable<Refed> {

                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();

        List<MethodSpec> methods = env.getOutputFor("org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(methods).anySatisfy(method -> {
            assertThat(method.name()).isEqualTo("iterator");
            assertThat(method.returnType().toString()).isEqualTo(Iterator.class.getName() + "<org.acme.mutiny.Refed>");
        });
        assertThat(methods).anySatisfy(method -> {
            assertThat(method.name()).isEqualTo("toMulti");
            assertThat(method.returnType().toString()).isEqualTo(Multi.class.getName() + "<org.acme.mutiny.Refed>");
        });
    }

    @Test
    void testIterableOfString() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyInterface", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface MyInterface extends Iterable<String> {

                }
                """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();

        List<MethodSpec> methods = env.getOutputFor("org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(methods).anySatisfy(method -> {
            assertThat(method.name()).isEqualTo("iterator");
            assertThat(method.returnType().toString()).isEqualTo(Iterator.class.getName() + "<java.lang.String>");
        });
        assertThat(methods).anySatisfy(method -> {
            assertThat(method.name()).isEqualTo("toMulti");
            assertThat(method.returnType().toString()).isEqualTo(Multi.class.getName() + "<java.lang.String>");
        });
    }

}
