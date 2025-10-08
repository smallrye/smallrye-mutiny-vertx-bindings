package tck;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.extra.mutiny.StaticMethodWithFuture;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StaticMethodsTest {

    @Test
    public void testInvokeStaticDoSomethingAsync() {
        UniAssertSubscriber<Void> assertSubscriber = StaticMethodWithFuture.doSomethingAsync()
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertSubscriber.assertCompleted();
    }

    @Test
    public void testAsyncCreate() {
        UniAssertSubscriber<StaticMethodWithFuture> assertSubscriber = StaticMethodWithFuture.asyncCreate()
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        StaticMethodWithFuture instance = assertSubscriber.assertCompleted().getItem();
        assertEquals("Hello static methods!", instance.sayHello());
    }
}
