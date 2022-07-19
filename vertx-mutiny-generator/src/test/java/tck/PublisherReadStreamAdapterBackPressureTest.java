package tck;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.vertx.MultiHelper;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.test.fakestream.FakeStream;
import org.junit.Test;
import java.util.concurrent.Flow.Publisher;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class PublisherReadStreamAdapterBackPressureTest extends ReadStreamAdapterBackPressureTest<Publisher<Buffer>> {

    @Override
    protected long defaultMaxBufferSize() {
        return 256;
    }

    @Override
    protected Publisher<Buffer> toObservable(ReadStream<Buffer> stream) {
        return MultiHelper.toMulti(stream);
    }

    @Override
    protected Publisher<Buffer> toObservable(ReadStream<Buffer> stream, int maxBufferSize) {
        return MultiHelper.toMulti(stream);
    }

    @Override
    protected Publisher<Buffer> flatMap(Publisher<Buffer> obs, Function<Buffer, Publisher<Buffer>> f) {
        return Multi.createFrom().publisher(obs).flatMap(f);
    }

    @Override
    protected void subscribe(Publisher<Buffer> obs, TestSubscriber<Buffer> sub) {
        TestUtils.subscribe(obs, sub);
    }

    @Override
    protected Publisher<Buffer> concat(Publisher<Buffer> obs1, Publisher<Buffer> obs2) {
        return Multi.createBy().concatenating().streams(obs1, obs2);
    }

    @Test
    public void testSubscribeTwice() {
        FakeStream<Buffer> stream = new FakeStream<>();
        Publisher<Buffer> observable = toObservable(stream);
        TestSubscriber<Buffer> subscriber1 = new TestSubscriber<Buffer>().prefetch(0);
        TestSubscriber<Buffer> subscriber2 = new TestSubscriber<Buffer>().prefetch(0);
        subscribe(observable, subscriber1);
        subscribe(observable, subscriber2);
        subscriber2.assertError(err -> {
            assertTrue(err instanceof IllegalStateException);
        });
        subscriber2.assertEmpty();
    }

    @Test
    public void testHandlerIsSetInDoOnSubscribe() {
        AtomicBoolean handlerSet = new AtomicBoolean();
        FakeStream<Buffer> stream = new FakeStream<Buffer>() {
            @Override
            public FakeStream<Buffer> handler(Handler<Buffer> handler) {
                handlerSet.set(true);
                return super.handler(handler);
            }
        };
        Multi<Buffer> observable = Multi.createFrom().publisher(toObservable(stream))
                .onSubscribe().invoke(s -> assertTrue(handlerSet.get()));
        TestSubscriber<Buffer> subscriber = new TestSubscriber<>();
        subscribe(observable, subscriber);
        subscriber.assertEmpty();
    }
}
