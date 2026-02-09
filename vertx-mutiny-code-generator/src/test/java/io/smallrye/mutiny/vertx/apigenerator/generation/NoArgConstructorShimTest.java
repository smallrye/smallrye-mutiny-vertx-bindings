package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NoArgConstructorShimTest {

    @Test
    void testNoArgConstructorWithoutTypeParameters()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Env env = new Env()
                .addModuleGen("org.acme", "test")
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyInterface {

                        }
                        """);
        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        env.compile();

        Constructor<?> constructor = env.getClass("org.acme.mutiny.MyInterface").getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThat(constructor).isNotNull();
        Object o = constructor.newInstance();
        assertThat(o.getClass().getName()).isEqualTo("org.acme.mutiny.MyInterface");

    }

    @Test
    void testNoArgConstructorWithTypeParameters()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Env env = new Env()
                .addModuleGen("org.acme", "test")
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyInterface<A, B> {

                        }
                        """);
        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        env.compile();

        Constructor<?> constructor = env.getClass("org.acme.mutiny.MyInterface").getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThat(constructor).isNotNull();
        Object o = constructor.newInstance();
        assertThat(o.getClass().getName()).isEqualTo("org.acme.mutiny.MyInterface");

    }

}
