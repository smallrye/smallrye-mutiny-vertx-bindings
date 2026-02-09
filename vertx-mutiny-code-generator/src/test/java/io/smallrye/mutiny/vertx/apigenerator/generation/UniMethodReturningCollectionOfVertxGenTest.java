package io.smallrye.mutiny.vertx.apigenerator.generation;

import com.palantir.javapoet.MethodSpec;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UniMethodReturningCollectionOfVertxGenTest {

    @Test
    void simple() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {

                        }
                        """)
                .addJavaCode("org.acme", "MyInterface",
                        """
                                package org.acme;

                                import io.vertx.codegen.annotations.VertxGen;
                                import java.util.List;
                                import java.util.Map;
                                import java.util.Set;
                                import io.vertx.core.Future;

                                @VertxGen
                                public interface MyInterface {
                                    Future<List<Refed>> returnListOfRefed();
                                    Future<Set<Refed>> returnSetOfRefed();

                                    // Test params
                                    Future<List<Refed>> acceptRefed(Refed refed);
                                    Future<List<Refed>> acceptListOfRefed(List<Refed> refed);
                                    Future<List<Refed>> acceptSetOfRefed(Set<Refed> refed);
                                    Future<List<Refed>> acceptMapOfRefed(java.util.Map<String, Refed> refed);
                                    Future<List<Refed>> acceptBasicTypes(int i, String s, List<Integer> list, Set<String> set, Map<String, String> map);

                                    // TODO Test other parameter types (Handler, Supplier, etc.)
                                }
                                """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = Env.getOutputFor(outputs, "org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).extracting("name").contains("returnListOfRefed", "returnSetOfRefed",
                "acceptRefed", "acceptListOfRefed", "acceptSetOfRefed", "acceptMapOfRefed", "acceptBasicTypes",
                "acceptMapOfRefedAndAwait", "acceptMapOfRefedAndForget");
    }

    @Test
    void withTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed<T> {

                        }
                        """)
                .addJavaCode("org.acme", "MyInterface",
                        """
                                package org.acme;

                                import io.vertx.codegen.annotations.VertxGen;
                                import java.util.List;
                                import java.util.Map;
                                import java.util.Set;
                                import io.vertx.core.Future;

                                @VertxGen
                                public interface MyInterface<T> {
                                    Future<List<Refed<T>>> returnListOfRefed();
                                    Future<Set<Refed<T>>> returnSetOfRefed();

                                    // Test params
                                    Future<List<Refed<T>>> acceptRefed(Refed<T> refed);
                                    Future<List<Refed<T>>> acceptListOfRefed(List<Refed<T>> refed);
                                    Future<List<Refed<T>>> acceptSetOfRefed(Set<Refed<T>> refed);
                                    Future<List<Refed<T>>> acceptMapOfRefed(java.util.Map<String, Refed<T>> refed);
                                    Future<List<Refed<T>>> acceptBasicTypes(int i, String s, List<Integer> list, Set<String> set, Map<String, String> map);

                                    // TODO Test other parameter types (Handler, Supplier, etc.)
                                }
                                """);
        env.addModuleGen("org.acme", "test");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();

        List<MethodSpec> specs = Env.getOutputFor(outputs, "org.acme.MyInterface").javaFile().typeSpec().methodSpecs();
        assertThat(specs).extracting("name").contains("returnListOfRefed", "returnSetOfRefed",
                "acceptRefed", "acceptListOfRefed", "acceptSetOfRefed", "acceptMapOfRefed", "acceptBasicTypes",
                "acceptMapOfRefedAndAwait", "acceptMapOfRefedAndForget");
    }
}
