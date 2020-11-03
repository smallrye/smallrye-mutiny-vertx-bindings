package io.vertx.mutiny.test;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.file.AsyncFile;
import io.vertx.test.core.Repeat;
import io.vertx.test.core.TestUtils;
import io.vertx.test.core.VertxTestBase;

public class AsyncFileTest extends VertxTestBase {

    private Vertx vertx;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        vertx = new Vertx(super.vertx);
    }

    @Test
    @Repeat(times = 100)
    public void multiToAsyncFile() throws Exception {
        sourceToAsyncFile((chunks, asyncFile) -> chunks.onItem().call(asyncFile::write)
                .onItem().ignoreAsUni());
    }

    private void sourceToAsyncFile(BiFunction<Multi<Buffer>, AsyncFile, Uni<Void>> func) throws Exception {
        File file = TestUtils.tmpFile("txt");
        assertTrue(!file.exists() || file.delete());

        List<Byte> bytes = IntStream.range(0, 128 * 1024).boxed()
                .map(step -> (byte) TestUtils.randomChar())
                .collect(toList());

        Multi<Buffer> flow = Multi.createFrom().iterable(bytes)
                .groupItems().intoLists().of(256)
                .onItem().transform(ba -> {
                    Buffer buffer = Buffer.buffer();
                    ba.forEach(buffer::appendByte);
                    return buffer;
                });

        Uni<Void> writeToFile = vertx.fileSystem().open(file.toString(), new OpenOptions().setWrite(true))
                .onItem().transformToUni(asyncFile -> func.apply(flow, asyncFile));

        Buffer buffer = writeToFile.chain(x -> vertx.fileSystem().readFile(file.toString()))
                .await().atMost(Duration.ofMinutes(1));

        assertEquals(buffer, bytes.stream().collect(Buffer::buffer, Buffer::appendByte, Buffer::appendBuffer));
    }
}
