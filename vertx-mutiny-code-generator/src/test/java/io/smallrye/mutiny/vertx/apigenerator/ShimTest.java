package io.smallrye.mutiny.vertx.apigenerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;

public class ShimTest {

    @Test
    void testParentClass() {
        MutinyGenerator generator = new MutinyGenerator(Paths.get("src/test/java"), "parent");
        List<ShimClass> shims = generator.analyze().shims();
        assertThat(shims).hasSize(2);
        ShimClass shim = generator.analyze().getShimFor("io.vertx.sources.parent.mutiny.InterfaceB");
        assertThat(shim).isNotNull();
        assertThat(shim.getPackage()).isEqualTo("io.vertx.sources.parent.mutiny");
        assertThat(shim.getFullyQualifiedName()).isEqualTo("io.vertx.sources.parent.mutiny.InterfaceB");
        assertThat(shim.getParentClass().asString()).isEqualTo("io.vertx.sources.parent.mutiny.InterfaceA");

        shim = generator.analyze().getShimFor("io.vertx.sources.parent.mutiny.InterfaceA");
        assertThat(shim).isNotNull();
        assertThat(shim.getPackage()).isEqualTo("io.vertx.sources.parent.mutiny");
        assertThat(shim.getFullyQualifiedName()).isEqualTo("io.vertx.sources.parent.mutiny.InterfaceA");
        assertThat(shim.getParentClass()).isNull();
    }

    @Test
    void testInterfaces() {
        MutinyGenerator generator = new MutinyGenerator(Paths.get("src/test/java"), "vertx-itf");
        List<ShimClass> shims = generator.analyze().shims();
        assertThat(shims).hasSize(3);

        ShimClass shim = generator.analyze().getShimFor("io.vertx.sources.itf.mutiny.StringInterface");
        assertThat(shim).isNotNull();
        assertThat(shim.getInterfaces())
                .anySatisfy(t -> assertThat(t.asString()).isEqualTo("io.vertx.core.Handler<java.lang.String>"))
                .anySatisfy(t -> assertThat(t.asString()).isEqualTo("java.util.Iterator<java.lang.String>"))
                .anySatisfy(t -> assertThat(t.asString()).isEqualTo("java.lang.Iterable<java.lang.String>"))
                .anySatisfy(t -> assertThat(t.asString())
                        .isEqualTo("java.util.function.Function<java.lang.String,java.lang.String>"));

        shim = generator.analyze().getShimFor("io.vertx.sources.itf.mutiny.DataObjectInterface");
        assertThat(shim).isNotNull();
        assertThat(shim.getInterfaces())
                .anySatisfy(t -> assertThat(t.asString()).isEqualTo("io.vertx.core.Handler<io.vertx.sources.itf.MyDataObject>"))
                .anySatisfy(t -> assertThat(t.asString()).isEqualTo("java.util.Iterator<io.vertx.sources.itf.MyDataObject>"))
                .anySatisfy(t -> assertThat(t.asString()).isEqualTo("java.lang.Iterable<io.vertx.sources.itf.MyDataObject>"))
                .anySatisfy(t -> assertThat(t.asString())
                        .isEqualTo("java.util.function.Function<java.lang.String,io.vertx.sources.itf.MyDataObject>"));
        shim = generator.analyze().getShimFor("io.vertx.sources.itf.mutiny.VertxGenInterface");
        assertThat(shim).isNotNull();
        assertThat(shim.getInterfaces())
                .anySatisfy(t -> assertThat(t.asString())
                        .isEqualTo("io.vertx.core.Handler<io.vertx.sources.itf.mutiny.StringInterface>"))
                .anySatisfy(t -> assertThat(t.asString())
                        .isEqualTo("java.util.Iterator<io.vertx.sources.itf.mutiny.StringInterface>"))
                .anySatisfy(t -> assertThat(t.asString())
                        .isEqualTo("java.lang.Iterable<io.vertx.sources.itf.mutiny.StringInterface>"))
                .anySatisfy(t -> assertThat(t.asString()).isEqualTo(
                        "java.util.function.Function<java.lang.String,io.vertx.sources.itf.mutiny.StringInterface>"));
    }

}
