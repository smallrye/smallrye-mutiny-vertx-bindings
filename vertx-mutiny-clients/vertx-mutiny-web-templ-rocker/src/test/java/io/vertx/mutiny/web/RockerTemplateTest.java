package io.vertx.mutiny.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.templ.rocker.RockerTemplateEngine;

public class RockerTemplateTest {

    static private Vertx vertx;

    @BeforeAll
    public static void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    public static void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testTemplate() {
        RockerTemplateEngine engine = RockerTemplateEngine.create();

        Buffer buffer = engine.render(new JsonObject().put("foo", "hello"), "templates/MyTemplate.rocker.html")
                .await().indefinitely();
        assertThat(buffer.toString()).contains("hello").doesNotContain("foo");
    }

}
