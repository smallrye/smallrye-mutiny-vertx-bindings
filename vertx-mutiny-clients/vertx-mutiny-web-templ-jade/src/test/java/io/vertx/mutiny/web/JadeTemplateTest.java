package io.vertx.mutiny.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.templ.jade.JadeTemplateEngine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JadeTemplateTest {

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
        JadeTemplateEngine engine = JadeTemplateEngine.create(vertx);
        Buffer buffer = engine.render(new JsonObject().put("foo", "hello"), "template.jade").await()
                .indefinitely();
        assertThat(buffer.toString()).contains("hello").doesNotContain("foo");
    }

}
