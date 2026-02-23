package io.vertx.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.impl.Utils;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import io.vertx.mutiny.core.file.AsyncFile;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientResponse;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.core.http.WebSocket;
import io.vertx.mutiny.core.http.WebSocketClient;

public class CoreTest {

    private static final String DEFAULT_FILE_PERMS = "rw-r--r--";
    private Vertx vertx;
    private String pathSep;
    private String testDir;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        java.nio.file.FileSystem fs = FileSystems.getDefault();
        pathSep = fs.getSeparator();
        File ftestDir = new File("target/temp/" + System.nanoTime());
        ftestDir.mkdirs();
        testDir = ftestDir.toString();
    }

    @AfterEach
    public void tearDown() {
        if (vertx != null) {
            vertx.closeAndAwait();
        }
    }

    /**
     * Creates a random string of ascii alpha characters
     *
     * @param length the length of the string to create
     * @return a String of random ascii alpha characters
     */
    public static String randomAlphaString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = (char) (65 + 25 * Math.random());
            builder.append(c);
        }
        return builder.toString();
    }

    @Test
    public void testAsyncFile() throws Exception {
        String fileName = "some-file.dat";
        int chunkSize = 1000;
        int chunks = 10;
        byte[] expected = randomAlphaString(chunkSize * chunks).getBytes();
        createFile(fileName, expected);
        AsyncFile af = vertx.fileSystem().open(testDir + pathSep + fileName, new OpenOptions()).await().indefinitely();
        readFileAndCheck(expected, af, 3);
    }

    @Test
    public void testAsyncFileBlocking() throws Exception {
        String fileName = "some-file.dat";
        int chunkSize = 1000;
        int chunks = 10;
        byte[] expected = randomAlphaString(chunkSize * chunks).getBytes();
        createFile(fileName, expected);
        Buffer buffer = Buffer.buffer();
        AsyncFile asyncFile = vertx.fileSystem().openAndAwait(testDir + pathSep + fileName, new OpenOptions());
        asyncFile.toBlockingStream().forEach(buffer::appendBuffer);
        assertEquals(Buffer.buffer(expected), buffer);
    }

    @Test
    public void testAndAwaitMethod() {
        EventBus eventBus = vertx.eventBus();
        eventBus.consumer("my-address").handler(m -> m.getDelegate().reply(m.body() + " world"));
        Message<Object> message = eventBus.requestAndAwait("my-address", "hello");
        assertEquals("hello world", message.body());
    }

    @Test
    public void testWebSocket() {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicLong serverReceived = new AtomicLong();
        // Set a 2 seconds timeout to force a TCP connection close
        int port = vertx.createHttpServer(new HttpServerOptions().setIdleTimeout(2)).webSocketHandler(ws -> {
            ws.toMulti()
                    .subscribe().with(msg -> {
                        serverReceived.incrementAndGet();
                        ws.writeTextMessageAndForget("pong");
                        latch.countDown();
                    }, (t -> {
                        fail(t.getMessage());
                        latch.countDown();
                    }));
        }).listenAndAwait(0, "localhost").actualPort();

        WebSocketClient client = vertx.createWebSocketClient();
        AtomicLong clientReceived = new AtomicLong();

        client.connect(port, "localhost", "/")
                .onItem().call(ws -> ws.writeTextMessage("ping"))
                .onItem().transformToMulti(WebSocket::toMulti)
                .subscribe().with(
                        msg -> {
                            clientReceived.incrementAndGet();
                            latch.countDown();
                        }, (t -> {
                            fail(t.getMessage());
                            latch.countDown();
                        }));
        await().until(() -> latch.getCount() == 0);
        assertEquals(1, serverReceived.get());
        assertEquals(1, clientReceived.get());
    }

    @Test
    public void testTimer() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        vertx.setTimer(10, x -> latch.countDown());

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testPeriodicTimer() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        vertx.setPeriodic(10, x -> latch.countDown());

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    private void readFileAndCheck(byte[] expected, AsyncFile file, int times) {
        for (int i = 0; i < times; i++) {
            file.setReadPos(0);
            Buffer actual = Buffer.buffer();
            file.toMulti()
                    .onItem().invoke(actual::appendBuffer)
                    .onItem().ignoreAsUni()
                    .await().indefinitely();

            assertThat(actual.getBytes()).isEqualTo(expected);
        }
    }

    private void createFile(String fileName, byte[] bytes) throws Exception {
        File file = new File(testDir, fileName);
        Path path = Paths.get(file.getCanonicalPath());
        Files.write(path, bytes);
        setDefaultPerms(path);
    }

    private void setDefaultPerms(Path path) {
        if (!Utils.isWindows()) {
            try {
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(DEFAULT_FILE_PERMS));
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    @Test
    public void testFollowRedirect() {
        CountDownLatch latch = new CountDownLatch(1);
        HttpServerOptions options = new HttpServerOptions().setPort(8081).setHost("localhost");
        AtomicInteger redirects = new AtomicInteger();
        HttpServer server = vertx.createHttpServer(options)
                .requestHandler(req -> {
                    redirects.incrementAndGet();
                    req.response().setStatusCode(301)
                            .putHeader(io.vertx.core.http.HttpHeaders.LOCATION.toString(),
                                    "http://localhost:" + 8082 + "/whatever")
                            .endAndForget();
                });
        server.listenAndAwait();

        HttpServer server2 = vertx.createHttpServer(options.setPort(8082));
        server2.requestHandler(req -> {
            assertEquals(1, redirects.get());
            assertEquals("http://localhost:" + 8082 + "/custom", req.absoluteURI());
            req.response().endAndForget();
            latch.countDown();
        });
        server2.listenAndAwait();

        HttpClient client = vertx.httpClientBuilder().withRedirectHandler(resp -> Uni.createFrom().emitter(e -> {
            vertx.setTimer(25, tick -> e.complete(new RequestOptions()
                    .setAbsoluteURI("http://localhost:" + 8082 + "/custom")));
        })).build();

        HttpClientResponse response = client.request(new RequestOptions().setHost("localhost").setPort(8081))
                .onItem().transformToUni(req -> {
                    req.setFollowRedirects(true);
                    return req.send();
                })
                .await().indefinitely();
        assertEquals("http://localhost:" + 8082 + "/custom", response.request().absoluteURI());

    }
}
