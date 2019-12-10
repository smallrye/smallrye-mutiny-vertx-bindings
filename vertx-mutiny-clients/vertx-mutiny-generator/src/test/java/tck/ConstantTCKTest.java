package tck;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ConstantTCKTest {

    @Test
    public void testBasic() {
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BYTE, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BYTE);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_BYTE, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_BYTE);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.SHORT, io.vertx.mutiny.codegen.testmodel.ConstantTCK.SHORT);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_SHORT, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_SHORT);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.INT, io.vertx.mutiny.codegen.testmodel.ConstantTCK.INT);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_INT, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_INT);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.LONG, io.vertx.mutiny.codegen.testmodel.ConstantTCK.LONG);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_LONG, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_LONG);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.FLOAT, io.vertx.mutiny.codegen.testmodel.ConstantTCK.FLOAT, 0.1);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_FLOAT, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_FLOAT, 0.1);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.DOUBLE, io.vertx.mutiny.codegen.testmodel.ConstantTCK.DOUBLE, 0.1);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_DOUBLE, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_DOUBLE, 0.1);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOOLEAN, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOOLEAN);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_BOOLEAN, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_BOOLEAN);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.CHAR, io.vertx.mutiny.codegen.testmodel.ConstantTCK.CHAR);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.BOXED_CHAR, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_CHAR);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.STRING, io.vertx.mutiny.codegen.testmodel.ConstantTCK.STRING);
    }

    @Test
    public void testVertxGen() {
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.VERTX_GEN, io.vertx.mutiny.codegen.testmodel.ConstantTCK.VERTX_GEN.getDelegate());
    }

    @Test
    public void testJson() {
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_OBJECT, io.vertx.mutiny.codegen.testmodel.ConstantTCK.JSON_OBJECT);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_ARRAY, io.vertx.mutiny.codegen.testmodel.ConstantTCK.JSON_ARRAY);
    }

    @Test
    public void testDataObject() {
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.DATA_OBJECT, io.vertx.mutiny.codegen.testmodel.ConstantTCK.DATA_OBJECT);
    }

    @Test
    public void testEnum() {
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.ENUM, io.vertx.mutiny.codegen.testmodel.ConstantTCK.ENUM);
    }

    @Test
    public void testObject() {
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.OBJECT, io.vertx.mutiny.codegen.testmodel.ConstantTCK.OBJECT);
    }

    @Test
    public void testThrowable() {
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.THROWABLE, io.vertx.mutiny.codegen.testmodel.ConstantTCK.THROWABLE);
    }

    @Test
    public void testNullable() {
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.NULLABLE_NON_NULL, io.vertx.mutiny.codegen.testmodel.ConstantTCK.NULLABLE_NON_NULL.getDelegate());
        assertNull(io.vertx.mutiny.codegen.testmodel.ConstantTCK.NULLABLE_NULL);
    }

    @Test
    public void testList() {
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.BYTE_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BYTE_LIST);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.SHORT_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.SHORT_LIST);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.INT_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.INT_LIST);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.LONG_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.LONG_LIST);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.FLOAT_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.FLOAT_LIST);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.DOUBLE_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.DOUBLE_LIST);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.CHAR_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.CHAR_LIST);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.STRING_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.STRING_LIST);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.VERTX_GEN_LIST.size(), io.vertx.mutiny.codegen.testmodel.ConstantTCK.VERTX_GEN_LIST.size());
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.VERTX_GEN_LIST.get(0),
                io.vertx.mutiny.codegen.testmodel.ConstantTCK.VERTX_GEN_LIST.get(0).getDelegate());
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_OBJECT_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.JSON_OBJECT_LIST);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_ARRAY_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.JSON_ARRAY_LIST);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.DATA_OBJECT_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.DATA_OBJECT_LIST);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.ENUM_LIST, io.vertx.mutiny.codegen.testmodel.ConstantTCK.ENUM_LIST);
    }

    @Test
    public void testSet() {
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.BYTE_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.BYTE_SET);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.SHORT_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.SHORT_SET);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.INT_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.INT_SET);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.LONG_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.LONG_SET);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.FLOAT_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.FLOAT_SET);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.DOUBLE_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.DOUBLE_SET);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.CHAR_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.CHAR_SET);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.STRING_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.STRING_SET);
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.VERTX_GEN_SET.size(), io.vertx.mutiny.codegen.testmodel.ConstantTCK.VERTX_GEN_SET.size());
        assertEquals(io.vertx.codegen.testmodel.ConstantTCK.VERTX_GEN_SET.iterator().next(),
                io.vertx.mutiny.codegen.testmodel.ConstantTCK.VERTX_GEN_SET.iterator().next().getDelegate());
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_OBJECT_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.JSON_OBJECT_SET);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.JSON_ARRAY_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.JSON_ARRAY_SET);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.DATA_OBJECT_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.DATA_OBJECT_SET);
        assertSame(io.vertx.codegen.testmodel.ConstantTCK.ENUM_SET, io.vertx.mutiny.codegen.testmodel.ConstantTCK.ENUM_SET);
    }

    private <V> V checkMap(Map<String, V> map) {
        assertEquals(1, map.size());
        return map.get("foo");
    }

    @Test
    public void testMap() {
        assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.BYTE_MAP), io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_BYTE);
        assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.SHORT_MAP), io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_SHORT);
        assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.INT_MAP), io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_INT);
        assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.LONG_MAP), io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_LONG);
        assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.FLOAT_MAP), io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_FLOAT);
        assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.DOUBLE_MAP), io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_DOUBLE);
        assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.CHAR_MAP), io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_CHAR);
        assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.BOOLEAN_MAP), io.vertx.mutiny.codegen.testmodel.ConstantTCK.BOXED_BOOLEAN);
        assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.STRING_MAP), io.vertx.mutiny.codegen.testmodel.ConstantTCK.STRING);
        assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.JSON_OBJECT_MAP), io.vertx.mutiny.codegen.testmodel.ConstantTCK.JSON_OBJECT);
        assertEquals(checkMap(io.vertx.codegen.testmodel.ConstantTCK.JSON_ARRAY_MAP), io.vertx.mutiny.codegen.testmodel.ConstantTCK.JSON_ARRAY);
    }
}
