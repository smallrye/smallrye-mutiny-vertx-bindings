package io.vertx.mutiny.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;

/**
 * @author Thomas Segismont
 */
public class ToStringTest {

    @Test
    public void testBufferToString() {
        String string = "The quick brown fox jumps over the lazy dog";
        assertEquals(string, Buffer.buffer(string).toString());
    }
}
