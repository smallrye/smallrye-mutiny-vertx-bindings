package io.vertx.axle.test;

import io.vertx.axle.codegen.extra.Bar;
import io.vertx.axle.codegen.extra.Foo;
import io.vertx.axle.codegen.extra.Generic1;
import io.vertx.axle.codegen.extra.Generic2;
import io.vertx.axle.codegen.extra.NestedParameterizedType;
import org.junit.Test;

public class GenericsTest {

    @Test
    public void testNestedParameterizedTypes() {
        // Test we don't get class cast when types are unwrapped or rewrapped
        Generic2<Generic1<Foo>, Generic2<Foo, Bar>> o = NestedParameterizedType.someGeneric();
        Generic1<Foo> value1 = o.getValue1();
        Foo nested1 = value1.getValue();
        value1.setValue(nested1);
        Generic2<Foo, Bar> value2 = o.getValue2();
        o.setValue2(value2);
        Foo nested2 = value2.getValue1();
        value2.setValue1(nested2);
        Bar nested3 = value2.getValue2();
        value2.setValue2(nested3);
    }
}