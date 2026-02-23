package io.vertx.mutiny.test;

import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.file.AsyncFile;

public class AsyncFileTest {

    private Vertx vertx;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    public void tearDown() {
        if (vertx != null) {
            vertx.closeAndAwait();
        }
    }

    @RepeatedTest(100)
    void multiToAsyncFile() throws Exception {
        sourceToAsyncFile((chunks, asyncFile) -> chunks.onItem().call(asyncFile::write)
                .onItem().ignoreAsUni());
    }

    private static final Random random = new Random();

    public static char randomChar() {
        return (char) (random.nextInt(16));
    }

    private void sourceToAsyncFile(BiFunction<Multi<Buffer>, AsyncFile, Uni<Void>> func) throws Exception {
        File file = File.createTempFile("vertx-core", "txt");
        Assertions.assertTrue(!file.exists() || file.delete());

        List<Byte> bytes = IntStream.range(0, 128 * 1024).boxed()
                .map(step -> (byte) randomChar())
                .collect(toList());

        Multi<Buffer> flow = Multi.createFrom().iterable(bytes)
                .group().intoLists().of(256)
                .onItem().transform(ba -> {
                    Buffer buffer = Buffer.buffer();
                    ba.forEach(buffer::appendByte);
                    return buffer;
                });

        OpenOptions openOptions = new OpenOptions().setWrite(true);
        Uni<Void> writeToFile = vertx.fileSystem().open(file.toString(), openOptions)
                .onItem().transformToUni(asyncFile -> func.apply(flow, asyncFile));

        Buffer buffer = writeToFile.chain(x -> vertx.fileSystem().readFile(file.toString()))
                .await().atMost(Duration.ofMinutes(1));

        Assertions.assertEquals(buffer, bytes.stream().collect(Buffer::buffer, Buffer::appendByte, Buffer::appendBuffer));
    }

    @RepeatedTest(100)
    void testToSubscriber() throws IOException {
        File file = File.createTempFile("vertx-core", "txt");
        Assertions.assertTrue(!file.exists() || file.delete());

        List<Byte> bytes = IntStream.range(0, 128 * 1024).boxed()
                .map(step -> (byte) randomChar())
                .collect(toList());

        Multi<Buffer> flow = Multi.createFrom().iterable(bytes)
                .group().intoLists().of(256)
                .onItem().transform(ba -> {
                    Buffer buffer = Buffer.buffer();
                    ba.forEach(buffer::appendByte);
                    return buffer;
                });

        OpenOptions openOptions = new OpenOptions().setWrite(true);
        AsyncFile af = vertx.fileSystem().open(file.toString(), openOptions).await().indefinitely();

        flow.subscribe().withSubscriber(af.toSubscriber());

        // We can't know when writing is done, so we just wait for a while
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
            Buffer buffer = vertx.fileSystem().readFile(file.toString()).await().indefinitely();
            Assertions.assertEquals(buffer, bytes.stream().collect(Buffer::buffer, Buffer::appendByte, Buffer::appendBuffer));
        });

    }
}
