package io.vertx.mutiny.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.healthchecks.CheckResult;
import io.vertx.ext.healthchecks.Status;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.healthchecks.HealthChecks;

public class HealthCheckTest {

    private Vertx vertx;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testHealthCheck() {
        HealthChecks hc = HealthChecks.create(vertx);
        hc.register("health", Uni.createFrom().item(Status::OK));

        CheckResult health = hc.checkStatusAndAwait("health");
        assertTrue(health.getUp());
        assertTrue(health.getStatus().isOk());
        assertEquals("health", health.getId());
    }

    // TODO: bring back or move to vertx-web once migrated (see https://vertx.io/docs/guides/vertx-5-migration-guide/#_vert_x_health_check)
    //    @Test
    //    public void testHealthCheckWithVertxWeb() {
    //        HealthChecks hc = HealthChecks.create(vertx);
    //        HealthCheckHandler handler = HealthCheckHandler.createWithHealthChecks(hc);
    //        hc.register("test", Uni.createFrom().item(Status::OK));
    //
    //        Router router = Router.router(vertx);
    //        router.get("/health*").handler(handler::handle);
    //
    //        vertx.createHttpServer()
    //                .requestHandler(router::handle)
    //                .listenAndAwait(8085);
    //
    //        WebClient webClient = WebClient.create(vertx);
    //        HttpResponse<Buffer> response1 = webClient.getAbs("http://localhost:8085/health").send().await()
    //                .indefinitely();
    //
    //        assertEquals(response1.statusCode(), 200);
    //        assertEquals(response1.bodyAsJsonObject().getString("outcome"), "UP");
    //
    //        HttpResponse<Buffer> response2 = webClient.getAbs("http://localhost:8085/health/test").send().await()
    //                .indefinitely();
    //
    //        assertEquals(response2.statusCode(), 200);
    //        assertEquals(response2.bodyAsJsonObject().getString("outcome"), "UP");
    //    }

}
