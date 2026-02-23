package io.vertx.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.parsetools.JsonParser;

/**
 * Verify the method accepting a Publisher
 */
public class PublisherTest {

    private Vertx vertx;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    public void tearDown() {
        if (vertx != null) {
            vertx.closeAndAwait();
        }
    }

    @Test
    void testPublisher() {
        String json = "{\n" +
                "  \"foo\": \"bar\",\n" +
                "  \"abc\": \"baz\"\n" +
                "}";
        Multi<String> items = Multi.createFrom().items(json);
        JsonParser parser = JsonParser.newParser(items.map(Buffer::buffer))
                .exceptionHandler(t -> t.printStackTrace())
                .objectValueMode();
        JsonObject object = parser.toMulti().collect().asList().await().indefinitely().get(0).objectValue();
        assertThat(object.getString("foo")).isEqualTo("bar");
        assertThat(object.getString("abc")).isEqualTo("baz");
    }
}
