package io.smallrye.mutiny.vertx.core.verticle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.mutiny.core.Vertx;

public class AbstractVerticleTest {

    private Vertx vertx;

    @BeforeEach
    public void setup() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testSuccessfulSynchronousDeployment() {
        String deploymentId = vertx.deployVerticle(SyncVerticle.class.getName()).await().indefinitely();
        assertThat(deploymentId).isNotNull().isNotBlank();
        assertThat(SyncVerticle.DEPLOYED).isTrue();
        vertx.undeploy(deploymentId).await().indefinitely();
        assertThat(SyncVerticle.DEPLOYED).isFalse();
    }

    @Test
    public void testSuccessfulAsynchronousDeployment() {
        String deploymentId = vertx.deployVerticle(AsyncVerticle.class.getName()).await().indefinitely();
        assertThat(deploymentId).isNotNull().isNotBlank();
        assertThat(AsyncVerticle.DEPLOYED).isTrue();
        vertx.undeploy(deploymentId).await().indefinitely();
        assertThat(AsyncVerticle.DEPLOYED).isFalse();
    }

    @Test
    public void testVerticleFailingSynchronouslyOnStart() {
        assertThatThrownBy(() -> vertx.deployVerticle(VerticleFailingSynchronously.class.getName()).await().indefinitely())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testVerticleFailingSynchronouslyOnStop() {
        assertThatThrownBy(() -> {
            String deploymentId = vertx.deployVerticle(VerticleFailingSynchronouslyOnStop.class.getName()).await()
                    .indefinitely();
            vertx.undeploy(deploymentId).await().indefinitely();
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testVerticleFailingAsynchronouslyOnStart() {
        assertThatThrownBy(() -> vertx.deployVerticle(VerticleFailingAsynchronously.class.getName()).await().indefinitely())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testVerticleFailingAsynchronouslyOnStop() {
        assertThatThrownBy(() -> {
            String deploymentId = vertx.deployVerticle(VerticleFailingAsynchronouslyOnStop.class.getName())
                    .await().indefinitely();
            vertx.undeploy(deploymentId).await().indefinitely();
        }).isInstanceOf(NullPointerException.class);
    }

}
