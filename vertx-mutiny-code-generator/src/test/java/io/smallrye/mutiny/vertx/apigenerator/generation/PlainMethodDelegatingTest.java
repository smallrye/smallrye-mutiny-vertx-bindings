package io.smallrye.mutiny.vertx.apigenerator.generation;

import com.palantir.javapoet.MethodSpec;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PlainMethodDelegatingTest {

    @Test
    void simple() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyDataObject", """
                        package org.acme;

                        import io.vertx.codegen.annotations.DataObject;

                        @DataObject
                        public class MyDataObject {

                        }
                        """)
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;

                        @VertxGen
                        public interface MyInterface {
                            List<String> returnList();
                            Set<String> returnSet();
                            MyDataObject dataObject();
                            int returnPrimitive();
                            Map<String, MyDataObject> returnMap();
                            void voidMethod();
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = Env.getOutputFor(outputs, "org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).extracting("name").contains("returnList", "returnSet", "returnPrimitive", "returnMap", "dataObject",
                "voidMethod");

        env.compile();
    }

    @Test
    void withTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyDataObject", """
                        package org.acme;

                        import io.vertx.codegen.annotations.DataObject;

                        @DataObject
                        public class MyDataObject {

                        }
                        """)
                .addJavaCode("org.acme", "MyInterface", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;

                        @VertxGen
                        public interface MyInterface<T> {
                            List<T> returnList();
                            Set<T> returnSet();
                            T justT();
                            MyDataObject dataObject();
                            int returnPrimitive();
                            Map<String, T> returnMap();
                        }
                        """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = Env.getOutputFor(outputs, "org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).extracting("name").contains("justT", "returnList", "returnSet", "returnPrimitive", "returnMap",
                "dataObject");

        env.compile();
    }
}
