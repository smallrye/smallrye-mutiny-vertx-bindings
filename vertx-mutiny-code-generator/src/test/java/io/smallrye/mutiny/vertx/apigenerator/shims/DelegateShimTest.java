package io.smallrye.mutiny.vertx.apigenerator.shims;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class DelegateShimTest {

    @Test
    void testSimple() {
        MutinyGenerator generator = new MutinyGenerator(Paths.get("src/test/java"), "shim");
        ShimClass shim = generator.analyze().getShimFor("io.vertx.sources.shims.mutiny.SimpleVertxGenInterface");
        assertThat(shim).isNotNull();

        assertThat(shim.getFields())
                .anySatisfy(f -> {
                    assertThat(f.getName()).isEqualTo("delegate");
                    assertThat(f.getType().asString()).isEqualTo("io.vertx.sources.shims.SimpleVertxGenInterface");
                });

        assertThat(shim.getMethods())
                .anySatisfy(m -> {
                    assertThat(m.getName()).isEqualTo("getDelegate");
                    assertThat(m.getReturnType().asString()).isEqualTo("io.vertx.sources.shims.SimpleVertxGenInterface");
                    assertThat(m.getParameters()).isEmpty();
                });

    }

    @Test
    void testWithASingleGeneric() {
        MutinyGenerator generator = new MutinyGenerator(Paths.get("src/test/java"), "shim");
        ShimClass shim = generator.analyze().getShimFor("io.vertx.sources.shims.mutiny.VertxGenInterfaceWithTypeParam");
        assertThat(shim).isNotNull();

        assertThat(shim.getFields())
                .anySatisfy(f -> {
                    assertThat(f.getName()).isEqualTo("delegate");
                    assertThat(f.getType().asString()).isEqualTo("io.vertx.sources.shims.VertxGenInterfaceWithTypeParam<X>");
                })
                .anySatisfy(f -> {
                    assertThat(f.getName()).isEqualTo("__typeArg_0");
                    assertThat(f.getType().asString()).isEqualTo("io.smallrye.mutiny.vertx.TypeArg<X>");
                });

        assertThat(shim.getMethods())
                .anySatisfy(m -> {
                    assertThat(m.getName()).isEqualTo("getDelegate");
                    assertThat(m.getReturnType().asString())
                            .isEqualTo("io.vertx.sources.shims.VertxGenInterfaceWithTypeParam<X>");
                    assertThat(m.getParameters()).isEmpty();
                });

    }

    @Test
    void testWithTwoGenerics() {
        MutinyGenerator generator = new MutinyGenerator(Paths.get("src/test/java"), "shim");

        ShimClass shim = generator.analyze().getShimFor("io.vertx.sources.shims.mutiny.VertxGenInterfaceWithTypeParams");
        assertThat(shim).isNotNull();

        assertThat(shim.getFields())
                .anySatisfy(f -> {
                    assertThat(f.getName()).isEqualTo("delegate");
                    assertThat(f.getType().asString()).isEqualTo("io.vertx.sources.shims.VertxGenInterfaceWithTypeParams<X,Y>");
                })
                .anySatisfy(f -> {
                    assertThat(f.getName()).isEqualTo("__typeArg_0");
                    assertThat(f.getType().asString()).isEqualTo("io.smallrye.mutiny.vertx.TypeArg<X>");
                })
                .anySatisfy(f -> {
                    assertThat(f.getName()).isEqualTo("__typeArg_1");
                    assertThat(f.getType().asString()).isEqualTo("io.smallrye.mutiny.vertx.TypeArg<Y>");
                });

        assertThat(shim.getMethods())
                .anySatisfy(m -> {
                    assertThat(m.getName()).isEqualTo("getDelegate");
                    assertThat(m.getReturnType().asString())
                            .isEqualTo("io.vertx.sources.shims.VertxGenInterfaceWithTypeParams<X,Y>");
                    assertThat(m.getParameters()).isEmpty();
                });
    }

}
