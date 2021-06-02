package io.vertx.mutiny.junit5;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.mutiny.core.Vertx;

@ExtendWith(VertxExtension.class)
class VertxParameterProviderTest {

    @Test
    void smokeTest(Vertx vertx, VertxTestContext testContext) {
        assertNotNull(vertx);
        testContext.completeNow();
    }
}
