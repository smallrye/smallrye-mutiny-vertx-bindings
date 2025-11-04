package tck;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StreamTest {

    @Test
    public void testStream() {
        org.extra.IterableWithStreamMethod bareInstance = new org.extra.IterableWithStreamMethod.Impl();
        org.extra.mutiny.IterableWithStreamMethod rxInstance = org.extra.mutiny.IterableWithStreamMethod.newInstance(bareInstance);
        assertEquals(2, rxInstance.stream().count());
    }
}
