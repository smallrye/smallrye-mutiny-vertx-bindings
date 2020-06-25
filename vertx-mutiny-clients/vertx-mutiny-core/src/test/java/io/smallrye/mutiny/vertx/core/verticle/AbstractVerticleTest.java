package io.smallrye.mutiny.vertx.core.verticle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.mutiny.core.Vertx;

public class AbstractVerticleTest {

    private Vertx vertx;

    @Before
    public void setup() {
        vertx = Vertx.vertx();
    }

    @After
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

    @Test(expected = NullPointerException.class)
    public void testVerticleFailingSynchronouslyOnStart() {
        vertx.deployVerticle(VerticleFailingSynchronously.class.getName()).await().indefinitely();
    }

    @Test(expected = NullPointerException.class)
    public void testVerticleFailingSynchronouslyOnStop() {
        String deploymentId = vertx.deployVerticle(VerticleFailingSynchronouslyOnStop.class.getName()).await().indefinitely();
        vertx.undeploy(deploymentId).await().indefinitely();
    }

    @Test(expected = NullPointerException.class)
    public void testVerticleFailingAsynchronouslyOnStart() {
        vertx.deployVerticle(VerticleFailingAsynchronously.class.getName()).await().indefinitely();
    }

    @Test(expected = NullPointerException.class)
    public void testVerticleFailingAsynchronouslyOnStop() {
        String deploymentId = vertx.deployVerticle(VerticleFailingAsynchronouslyOnStop.class.getName())
                .await().indefinitely();
        vertx.undeploy(deploymentId).await().indefinitely();
    }

}
