package io.smallrye.mutiny.vertx.apigenerator.generation;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class MethodWithTypeParameterTest {

    @Test
    public void test() {
        Env env = new Env()
                .addJavaCode("org.acme", "AsyncMap.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface AsyncMap<K, V> {
                            K key();
                            V value();
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import java.util.function.Function;
                        import io.vertx.core.Future;

                        @VertxGen
                        public interface MyInterface {
                            <K, V> Future<AsyncMap<K,V>> method();
                        }
                        """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());
        env.compile();
    }

}
