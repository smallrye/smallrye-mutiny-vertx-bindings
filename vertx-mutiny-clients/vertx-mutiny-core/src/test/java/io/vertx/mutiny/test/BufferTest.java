package io.vertx.mutiny.test;

import static junit.framework.TestCase.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.vertx.core.buffer.Buffer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BufferTest {

    ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper(new YAMLFactory());
    }

    @Test
    public void testClusterSerializable() {
        io.vertx.mutiny.core.buffer.Buffer buff = io.vertx.mutiny.core.buffer.Buffer.buffer("hello-world");
        Buffer actual = Buffer.buffer();
        buff.writeToBuffer(actual);
        Buffer expected = Buffer.buffer();
        Buffer.buffer("hello-world").writeToBuffer(expected);
        assertEquals(expected, actual);
        buff = io.vertx.mutiny.core.buffer.Buffer.buffer("hello-world");
        assertEquals(expected.length(), buff.readFromBuffer(0, expected));
        assertEquals("hello-world", buff.toString());
    }
}
