package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

public class DelegateWithConcreteParentTest {

    @Test
    void test() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyParent", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen(concrete=false)
                        public interface MyParent<T> {
                            T foo();
                        }
                        """)
                .addJavaCode("org.acme", "MyChild", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyChild<T> extends MyParent<Container<T>>{
                            String invoke(T t);
                        }
                        """)
                .addJavaCode("org.acme", "Container", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Container<T> {
                            T body();
                        }
                        """)
                .addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
    }
}
