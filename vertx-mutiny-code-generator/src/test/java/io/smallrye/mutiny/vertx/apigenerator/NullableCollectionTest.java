package io.smallrye.mutiny.vertx.apigenerator;

import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenMethod;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NullableCollectionTest {

    @Test
    void testNullableOnReturnType() {
        Env env = new Env();
        env.addModuleGen("org.acme", "org.acme", "foo");
        env.addJavaCode("org.acme", "Foo", """
                package org.acme.foo;

                import io.vertx.codegen.annotations.VertxGen;
                import io.vertx.codegen.annotations.Nullable;

                @VertxGen
                public interface Foo {
                    @Nullable String foo();
                    String bar();
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        var vg = generator.getCollectionResult().getInterface("org.acme.foo.Foo");
        VertxGenMethod method = vg.getMethod("foo");
        assertThat(method).isNotNull();
        assertThat(method.isReturnTypeNullable()).isTrue();

        method = vg.getMethod("bar");
        assertThat(method).isNotNull();
        assertThat(method.isReturnTypeNullable()).isFalse();

    }

    @Test
    void testNullableOnParameter() {
        Env env = new Env();
        env.addModuleGen("org.acme", "org.acme", "foo");
        env.addJavaCode("org.acme", "Foo", """
                package org.acme.foo;

                import io.vertx.codegen.annotations.VertxGen;
                import io.vertx.codegen.annotations.Nullable;

                @VertxGen
                public interface Foo {
                    String foo(@Nullable String s, int i);
                    String bar(String s, int i);
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        var vg = generator.getCollectionResult().getInterface("org.acme.foo.Foo");
        VertxGenMethod method = vg.getMethod("foo");
        assertThat(method).isNotNull();
        assertThat(method.getParameters().get(0).nullable()).isTrue();
        assertThat(method.getParameters().get(1).nullable()).isFalse();

        method = vg.getMethod("bar");
        assertThat(method).isNotNull();
        assertThat(method.getParameters().get(0).nullable()).isFalse();
        assertThat(method.getParameters().get(1).nullable()).isFalse();

    }

}
