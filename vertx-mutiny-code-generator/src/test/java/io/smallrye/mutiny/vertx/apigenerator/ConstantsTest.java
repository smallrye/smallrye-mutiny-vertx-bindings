package io.smallrye.mutiny.vertx.apigenerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenModule;

class ConstantsTest {

    VertxGenModule module = new VertxGenModule("ignored", "io.vertx", List.of("io.vertx"));

    @Test
    void testPackageNameGeneration() {
        assertThat(ShimConstants.getPackageName(module, "io.vertx.core")).isEqualTo("io.vertx.mutiny.core");
        assertThat(ShimConstants.getPackageName(module, "io.vertx.ext.mail")).isEqualTo("io.vertx.mutiny.ext.mail");
    }

    @Test
    void testClassNameGeneration() {
        assertThat(ShimConstants.getClassName(module, "io.vertx.core.Vertx")).isEqualTo("io.vertx.mutiny.core.Vertx");
        assertThat(ShimConstants.getClassName(module, "io.vertx.ext.mail.MailClient"))
                .isEqualTo("io.vertx.mutiny.ext.mail.MailClient");
    }

}
