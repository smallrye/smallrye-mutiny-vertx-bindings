package io.vertx.converters;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@VertxGen
public interface ConverterTestClass {

    void justVoid();

    Void boxedVoid();

    int primitiveInt();

    Integer boxedInt();

    String string();

    DummyDataObject dataObject();

    Class<String> classOfString();

    Class<DummyDataObject> classOfDataObject();

    <X> Class<X> classOfX();

    Class<?> justClass();

    DummyEnum enumValue();

    JsonArray jsonArray();

    JsonObject jsonObject();

    IllegalArgumentException throwable();

    List<String> listOfString();

    List<DummyDataObject> listOfDataObjects();

    List<ConverterTestClass> listOfVertxGen();

    Set<String> setOfString();

    Set<DummyDataObject> setOfDataObjects();

    Map<String, String> mapOfString();

    Map<String, DummyDataObject> mapOfStringDataObject();

    Map<String, ConverterTestClass> mapOfStringVertxGen();

    ConverterTestClass vertxGen();

    Future<String> futureOfString();

    Future<DummyDataObject> futureOfDataObject();

    Future<ConverterTestClass> futureOfVertxGen();

    Handler<Void> handlerOfVoid();

    Handler<String> handlerOfString();

    Handler<DummyDataObject> handlerOfDataObject();

    Handler<ConverterTestClass> handlerOfVertxGen();

    Handler<AsyncResult<ConverterTestClass>> handlerOfAsyncResultVertxGen();

    Handler<AsyncResult<DummyDataObject>> handlerOfAsyncResultDataObject();

    Function<String, String> functionOfString();

    Function<String, DummyDataObject> functionOfStringDataObject();

    Function<String, ConverterTestClass> functionOfStringVertxGen();

    Supplier<String> supplierOfString();

    Supplier<DummyDataObject> supplierOfDataObject();

    Supplier<ConverterTestClass> supplierOfVertxGen();
}
