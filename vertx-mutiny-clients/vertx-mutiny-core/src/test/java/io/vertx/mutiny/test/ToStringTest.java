package io.vertx.mutiny.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.net.SocketAddress;

/**
 * @author Thomas Segismont
 */
public class ToStringTest {

    @Test
    public void testBufferToString() {
        String string = "The quick brown fox jumps over the lazy dog";
        assertEquals(string, Buffer.buffer(string).toString());
    }

    @Test
    public void testSocketAddressToString() {
        io.vertx.core.net.SocketAddress socketAddress = new SocketAddressImpl(8888, "guest");
        SocketAddress rxSocketAddress = SocketAddress.newInstance(socketAddress);
        assertEquals(socketAddress.toString(), rxSocketAddress.toString());
    }
}
