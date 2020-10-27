package tck;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.streams.WriteStream;

/**
 * @author Thomas Segismont
 */
public class FakeWriteStream implements WriteStream<Integer> {

    private final io.vertx.core.Vertx vertx;

    private volatile int last = -1;
    private volatile boolean writeQueueFull;
    private volatile Handler<Void> drainHandler;
    private volatile boolean drainHandlerInvoked;
    private volatile Runnable onWrite;
    private volatile Handler<Throwable> exceptionHandler;
    private volatile Throwable failAfterWrite;
    private volatile boolean endInvoked;

    public FakeWriteStream(Vertx vertx) {
        this.vertx = vertx;
    }

    public boolean drainHandlerInvoked() {
        return drainHandlerInvoked;
    }

    @Override
    public FakeWriteStream exceptionHandler(Handler<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    @Override
    public Future<Void> write(Integer data) {
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }
        Runnable r = onWrite;
        if (r != null) {
            r.run();
        }
        if (data != last + 1) {
            throw new IllegalStateException("Expected " + (last + 1) + ", got " + data);
        }
        last = data;
        Throwable t = failAfterWrite;
        if (t != null) {
            vertx.runOnContext(v -> {
                Handler<Throwable> h = exceptionHandler;
                if (h != null) {
                    h.handle(t);
                }
            });
        }
        return Future.succeededFuture();
    }

    @Override
    public void write(Integer data, Handler<AsyncResult<Void>> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Void> end() {
        endInvoked = true;
        return Future.succeededFuture();
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
        end().onComplete(handler);
    }

    @Override
    public FakeWriteStream setWriteQueueMaxSize(int maxSize) {
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        if (last % 4 == 0) {
            writeQueueFull = true;
            vertx.runOnContext(v -> {
                writeQueueFull = false;
                Handler<Void> h = drainHandler;
                if (h != null) {
                    drainHandlerInvoked = true;
                    h.handle(null);
                }
            });
            return true;
        }
        return writeQueueFull;
    }

    @Override
    public FakeWriteStream drainHandler(Handler<Void> drainHandler) {
        this.drainHandler = drainHandler;
        return this;
    }

    public FakeWriteStream setOnWrite(Runnable onWrite) {
        this.onWrite = onWrite;
        return this;
    }

    public FakeWriteStream failAfterWrite(Throwable expected) {
        failAfterWrite = expected;
        return this;
    }

    public int getCount() {
        return last + 1;
    }

    public boolean endInvoked() {
        return endInvoked;
    }
}
