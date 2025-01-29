package io.smallrye.mutiny.vertx.apigenerator.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenInterface;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenMethod;
import io.vertx.converters.ConverterTestClass;

public class ConverterTest {

    private static VertxGenInterface itf;
    private static MutinyGenerator generator;

    @BeforeAll
    static void init() {
        generator = new MutinyGenerator(Paths.get("src/test/java/"), "converters");
        itf = generator.getCollectionResult().getInterface(ConverterTestClass.class.getName());
    }

    @Test
    public void testVoid() {
        assertReturnedType("justVoid", "void");
        assertReturnedType("boxedVoid", "java.lang.Void");
    }

    @Test
    public void testPrimitive() {
        assertReturnedType("primitiveInt", "int");
        assertReturnedType("boxedInt", "java.lang.Integer");
    }

    @Test
    public void testString() {
        assertReturnedType("string", "java.lang.String");
    }

    @Test
    public void testDataObject() {
        assertReturnedType("dataObject", "io.vertx.converters.DummyDataObject");
    }

    @Test
    public void testClass() {
        assertReturnedType("classOfString", "java.lang.Class<java.lang.String>");
        assertReturnedType("classOfDataObject", "java.lang.Class<io.vertx.converters.DummyDataObject>");
        assertReturnedType("classOfX", "java.lang.Class<X>");
        assertReturnedType("justClass", "java.lang.Class<?>");
        // TODO Test with VertxGen
    }

    @Test
    public void testEnum() {
        assertReturnedType("enumValue", "io.vertx.converters.DummyEnum");
    }

    @Test
    public void testJson() {
        assertReturnedType("jsonArray", "io.vertx.core.json.JsonArray");
        assertReturnedType("jsonObject", "io.vertx.core.json.JsonObject");
    }

    @Test
    public void testThrowable() {
        assertReturnedType("throwable", "java.lang.IllegalArgumentException");
    }

    @Test
    public void testListConverter() {
        assertReturnedType("listOfString", "java.util.List<java.lang.String>");
        assertReturnedType("listOfDataObjects", "java.util.List<io.vertx.converters.DummyDataObject>");
        assertReturnedType("listOfVertxGen", "java.util.List<io.vertx.converters.mutiny.ConverterTestClass>");
    }

    @Test
    public void testSetConverter() {
        assertReturnedType("setOfString", "java.util.Set<java.lang.String>");
        assertReturnedType("setOfDataObjects", "java.util.Set<io.vertx.converters.DummyDataObject>");
    }

    @Test
    public void testMapConverter() {
        assertReturnedType("mapOfString", "java.util.Map<java.lang.String,java.lang.String>");
        assertReturnedType("mapOfStringDataObject", "java.util.Map<java.lang.String,io.vertx.converters.DummyDataObject>");
        assertReturnedType("mapOfStringVertxGen",
                "java.util.Map<java.lang.String,io.vertx.converters.mutiny.ConverterTestClass>");
    }

    @Test
    public void testVertxGen() {
        assertReturnedType("vertxGen", "io.vertx.converters.mutiny.ConverterTestClass");
    }

    @Test
    public void testFuture() {
        assertReturnedType("futureOfString", "io.smallrye.mutiny.Uni<java.lang.String>");
        assertReturnedType("futureOfDataObject", "io.smallrye.mutiny.Uni<io.vertx.converters.DummyDataObject>");
        assertReturnedType("futureOfVertxGen", "io.smallrye.mutiny.Uni<io.vertx.converters.mutiny.ConverterTestClass>");
    }

    @Test
    public void testHandler() {
        assertReturnedType("handlerOfVoid", Runnable.class.getName());
        assertReturnedType("handlerOfString", "java.util.function.Consumer<java.lang.String>");
        assertReturnedType("handlerOfDataObject", "java.util.function.Consumer<io.vertx.converters.DummyDataObject>");
        assertReturnedType("handlerOfVertxGen", "java.util.function.Consumer<io.vertx.converters.mutiny.ConverterTestClass>");
        assertReturnedType("handlerOfAsyncResultDataObject",
                "java.util.function.Consumer<io.vertx.core.AsyncResult<io.vertx.converters.DummyDataObject>>");
        assertReturnedType("handlerOfAsyncResultVertxGen",
                "java.util.function.Consumer<io.vertx.core.AsyncResult<io.vertx.converters.mutiny.ConverterTestClass>>");
    }

    @Test
    public void testFunction() {
        assertReturnedType("functionOfString", "java.util.function.Function<java.lang.String,java.lang.String>");
        assertReturnedType("functionOfStringDataObject",
                "java.util.function.Function<java.lang.String,io.vertx.converters.DummyDataObject>");
        assertReturnedType("functionOfStringVertxGen",
                "java.util.function.Function<java.lang.String,io.vertx.converters.mutiny.ConverterTestClass>");
    }

    @Test
    public void testSupplier() {
        assertReturnedType("supplierOfString", "java.util.function.Supplier<java.lang.String>");
        assertReturnedType("supplierOfDataObject", "java.util.function.Supplier<io.vertx.converters.DummyDataObject>");
        assertReturnedType("supplierOfVertxGen", "java.util.function.Supplier<io.vertx.converters.mutiny.ConverterTestClass>");
    }

    private void assertReturnedType(String methodName, String expected) {
        VertxGenMethod method = itf.getMethod(methodName);
        var res = generator.getConverters().convert(method.getReturnedType());
        assertThat(res.asString()).isEqualTo(expected);
    }

}
