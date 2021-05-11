package io.vertx.mutiny.sqlclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.function.Function;

import org.junit.Test;

import io.smallrye.mutiny.Uni;

public abstract class UsingConnectionSafetyTest {

    protected Pool pool;

    @Test
    public void testUsingConnectionMulti() throws Exception {
        doTest(throwable -> SqlClientHelper.usingConnectionMulti(pool, conn -> {
            throw throwable;
        }).collect().first());
    }

    @Test
    public void testUsingConnectionUni() throws Exception {
        doTest(throwable -> SqlClientHelper.usingConnectionUni(pool, conn -> {
            throw throwable;
        }));
    }

    private void doTest(Function<RuntimeException, Uni<Object>> function) {
        for (int i = 0; i < getMaxPoolSize() + 1; i++) {
            RuntimeException expected = new RuntimeException();
            try {
                function.apply(expected).await().indefinitely();
                fail("Should not complete succesfully");
            } catch (Exception e) {
                assertThat(e).isEqualTo(expected);
            }
        }
    }

    protected abstract int getMaxPoolSize();
}
