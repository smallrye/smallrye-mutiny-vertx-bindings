package io.vertx.mutiny.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.templ.pug.PugTemplateEngine;

public class PugTemplateTest {

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
        PugTemplateEngine engine = PugTemplateEngine.create(vertx);
        Buffer buffer = engine.renderAndAwait(new JsonObject().put("foo", "hello"), "template.pug");
        assertThat(buffer.toString()).contains("<p>hello</p>");
    }

}
