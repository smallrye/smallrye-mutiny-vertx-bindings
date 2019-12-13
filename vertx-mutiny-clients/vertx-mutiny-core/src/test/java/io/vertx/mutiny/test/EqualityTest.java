package io.vertx.mutiny.test;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.stream.Stream;

import org.junit.Test;

import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.net.SocketAddress;

/**
 * @author Thomas Segismont
 */
public class EqualityTest {

    @Test
    public void testBufferEquality() {
        Buffer buf1 = Buffer.buffer("The quick brown fox jumps over the lazy dog");
        Buffer buf2 = buf1.copy();
        assertNotSame(buf1, buf2);
        assertEquals(buf1, buf2);
    }

    @Test
    public void testBufferSet() {
        Buffer buf1 = Buffer.buffer("The quick brown fox jumps over the lazy dog");
        Buffer buf2 = buf1.copy();
        assertEquals(1, Stream.of(buf1, buf2).collect(toSet()).size());
    }

    @Test
    public void testSocketAddressEquality() {
        SocketAddress address1 = SocketAddress.newInstance(new SocketAddressImpl(8888, "guest"));
        SocketAddress address2 = SocketAddress.newInstance(new SocketAddressImpl(8888, "guest"));
        assertNotSame(address1, address2);
        assertEquals(address1, address2);
    }

    @Test
    public void testSocketAddressSet() {
        SocketAddress address1 = SocketAddress.newInstance(new SocketAddressImpl(8888, "guest"));
        SocketAddress address2 = SocketAddress.newInstance(new SocketAddressImpl(8888, "guest"));
        assertEquals(1, Stream.of(address1, address2).collect(toSet()).size());
    }
}
