package io.smallrye.mutiny.vertx.apigenerator;

import io.smallrye.mutiny.vertx.apigenerator.analysis.AnalysisResult;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenInterface;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenMethod;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class CollectionTest {

    @Test
    void testSimpleAPI() {
        MutinyGenerator generator = new MutinyGenerator(Paths.get("src/test/java/io/vertx/sources/simple"));
        List<VertxGenInterface> interfaces = generator.getCollectionResult().interfaces();
        assertThat(interfaces).hasSize(1);
        assertThat(interfaces.get(0).isConcrete()).isTrue();
        assertThat(interfaces.get(0).getFullyQualifiedName()).isEqualTo("io.vertx.sources.simple.TestApi");
        assertThat(interfaces.get(0).getPackageName()).isEqualTo("io.vertx.sources.simple");
        assertThat(interfaces.get(0).getMethods()).hasSize(1);
        assertThat(interfaces.get(0).getMethods().get(0)).satisfies(m -> {
            assertThat(m.getName()).isEqualTo("foo");
            assertThat(m.getReturnedType().isVoid()).isTrue();
            assertThat(m.getParameters()).isEmpty();
        });

        assertThat(generator.getCollectionResult().allModules()).hasSize(1);

        AnalysisResult analyzed = generator.analyze();
        assertThat(analyzed.shims()).hasSize(1);
        ShimClass shim = analyzed.shims().get(0);
        assertThat(shim.getFullyQualifiedName()).isEqualTo("io.vertx.sources.simple.mutiny.TestApi");
        assertThat(shim.getPackage()).isEqualTo("io.vertx.sources.simple.mutiny");

    }

    @Test
    void testDataObjectDetection() {
        MutinyGenerator generator = new MutinyGenerator(Paths.get("src/test/java/io/vertx/sources"), "simple");
        assertThat(generator.getCollectionResult().allDataObjects())
                .contains("io.vertx.sources.simple.dataobject.TestDataObject");
    }

    @Test
    void testConstantsCollection() {
        MutinyGenerator generator = new MutinyGenerator(Paths.get("src/test/java/io/vertx/sources/constants"));
        List<VertxGenInterface> interfaces = generator.getCollectionResult().interfaces();
        assertThat(interfaces).hasSize(2);
        VertxGenInterface found = generator.getCollectionResult().getInterface("io.vertx.sources.constants.ConstantTCK");
        assertThat(found.getConstants()).hasSize(65);
    }

    @Test
    void testParentCollection() {
        MutinyGenerator generator = new MutinyGenerator(Paths.get("src/test/java"), "parent");
        List<VertxGenInterface> interfaces = generator.getCollectionResult().interfaces();
        assertThat(interfaces).hasSize(2);
        VertxGenInterface found = generator.getCollectionResult().getInterface("io.vertx.sources.parent.InterfaceB");
        assertThat(found).isNotNull();
        assertThat(found.getMethods()).hasSize(1);
        assertThat(found.getMethods().stream().map(VertxGenMethod::getName).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("bar");

        ShimClass first = generator.analyze().getShimFor("io.vertx.sources.parent.mutiny.InterfaceB");
        assertThat(first).isNotNull();
        assertThat(first.getParentClass().asClassOrInterfaceType().getNameWithScope())
                .isEqualTo("io.vertx.sources.parent.mutiny.InterfaceA");

    }

    @Test
    void testModulesMerge() {
        Env env = new Env();
        env.addModuleGen("org.acme.foo", "org.acme", "foo");
        env.addModuleGen("org.acme.bar", "org.acme", "foo");
        env.addJavaCode("org.acme.foo", "Foo", """
                package org.acme.foo;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Foo {
                    void foo();
                }
                """);
        env.addJavaCode("org.acme.bar", "Bar", """
                package org.acme.foo;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Bar {
                    void bar();
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        assertThat(generator.getCollectionResult().allVertxGenClasses()).hasSize(2);
        assertThat(generator.getCollectionResult().allModules()).hasSize(1);

    }

}
