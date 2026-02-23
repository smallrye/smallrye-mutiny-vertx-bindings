package io.vertx.sources.constants;

import io.vertx.codegen.annotations.DataObject;

@DataObject
public class TestDataObject {

    private String foo;
    private int bar;
    private double wibble;

    public String getFoo() {
        return foo;
    }

    public TestDataObject setFoo(String foo) {
        this.foo = foo;
        return this;
    }
}