package io.smallrye.mutiny.vertx.apigenerator.shims;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;

public class FunctionShimTest {
    @Test
    public void testSimple() {

        MutinyGenerator generator = new MutinyGenerator(Paths.get("src/test/java/io/vertx/sources/shims"), "shim");

        ShimClass shim = generator.analyze()
                .getShimFor("io.vertx.sources.shims.mutiny.SimpleVertxGenInterfaceExtendingFunction");
        assertThat(shim).isNotNull();

        assertThat(shim.getMethods())
                .anySatisfy(m -> {
                    assertThat(m.getName()).isEqualTo("apply");
                    assertThat(m.getReturnType().asString()).isEqualTo("java.lang.String");
                    assertThat(m.getParameters()).hasSize(1);
                    assertThat(shim.isVertxGen(m.getParameters().get(0).originalType().asReferenceType().getQualifiedName()))
                            .isFalse();
                });
    }

    @Test
    public void testWithGenericParams() {
        MutinyGenerator generator = new MutinyGenerator(Paths.get("src/test/java/io/vertx/sources/shims"), "shim");

        ShimClass shim = generator.analyze().getShimFor("io.vertx.sources.shims.mutiny.FunctionInterfaceGenParameters");
        assertThat(shim).isNotNull();
        
        assertThat(shim.getMethods())
                .anySatisfy(m -> {
                    assertThat(m.getName()).isEqualTo("apply");
                    assertThat(m.getParameters()).hasSize(1);
                    assertThat(m.getReturnType().asString()).isEqualTo("io.vertx.sources.shims.mutiny.VertxGenType");
                    assertThat(shim.isVertxGen(m.getParameters().get(0).originalType().asReferenceType().getQualifiedName()))
                            .isTrue();
                });
    }
}
