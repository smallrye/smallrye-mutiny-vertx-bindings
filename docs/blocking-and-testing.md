# Blocking and Testing

The Mutiny Vert.x bindings provide blocking APIs that convert asynchronous `Uni` and `Multi` results into synchronous calls. These APIs are essential for testing and are equally valuable for imperative-style programming on virtual threads, worker threads, and CLI tools.

**Never block on the Vert.x event loop.** Blocking the event loop freezes the entire application. Use these APIs only in contexts where blocking is safe:

- **Virtual threads** (Java 21+): first-class imperative programming with Vert.x clients
- **Worker threads**: offloaded via `executeBlocking`
- **Tests**: JUnit test methods run outside the event loop
- **CLI tools and scripts**: short-lived programs without an event loop

## Virtual Threads: Imperative Vert.x

With Java 21+ virtual threads, the blocking APIs let you write Vert.x client code in a straightforward imperative style. Virtual threads can block without wasting platform threads, making `andAwait()` and `.await().atMost()` practical outside of tests.

For example, a web client call written imperatively:

=== "Mutiny Bindings"

    ```java
    WebClient client = WebClient.create(vertx, new WebClientOptions()
            .setDefaultPort(8080)
            .setDefaultHost("localhost"));

    HttpResponse<JsonObject> response = client.get("/api/users")
            .as(BodyCodec.jsonObject())
            .sendAndAwait();

    JsonObject body = response.body();
    ```

=== "Vanilla Vert.x"

    ```java
    WebClient client = WebClient.create(vertx, new WebClientOptions()
            .setDefaultPort(8080)
            .setDefaultHost("localhost"));

    HttpResponse<JsonObject> response = client.get("/api/users")
            .as(BodyCodec.jsonObject())
            .send()
            .toCompletionStage().toCompletableFuture().join();

    JsonObject body = response.body();
    ```

No callbacks, no reactive chains: just sequential code that reads top to bottom. Each `andAwait()` call suspends the virtual thread until the result is available, while the underlying Vert.x event loop continues processing other work.

## andAwait()

Methods ending in `andAwait()` block the calling thread indefinitely until the operation completes. Every `Uni`-returning method on a Mutiny Vert.x class has a corresponding `andAwait()` variant.

=== "Mutiny Bindings"

    ```java
    EventBus bus = vertx.eventBus();
    bus.consumer("address", message -> message.reply("world"))
            .completionAndAwait();

    Message<Object> reply = bus.requestAndAwait("address", "hello");
    ```

=== "Vanilla Vert.x"

    ```java
    EventBus bus = vertx.eventBus();
    bus.consumer("address", message -> message.reply("world"))
            .completion()
            .toCompletionStage().toCompletableFuture().join();

    Message<Object> reply = bus.request("address", "hello")
            .toCompletionStage().toCompletableFuture().join();
    ```

Use `andAwait()` when you are confident the operation will complete and do not need a timeout: for instance, in tests with controlled infrastructure or on virtual threads where indefinite suspension is acceptable.

## .await().atMost(Duration)

For operations that may hang or where you want a safety net, use `.await().atMost(Duration)` on the `Uni` directly. This blocks until the result arrives or the timeout expires, throwing a `TimeoutException` if the deadline is exceeded.

=== "Mutiny Bindings"

    ```java
    Buffer payload = vertx.createHttpClient()
            .request(HttpMethod.GET, port, "localhost", "/")
            .onItem().transformToUni(HttpClientRequest::send)
            .onItem().transformToUni(HttpClientResponse::body)
            .await().atMost(Duration.ofSeconds(5));
    ```

=== "Vanilla Vert.x"

    ```java
    Buffer payload = vertx.createHttpClient()
            .request(HttpMethod.GET, port, "localhost", "/")
            .compose(HttpClientRequest::send)
            .compose(HttpClientResponse::body)
            .toCompletionStage().toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    ```

This is the preferred approach when calling external services that might not respond.

## Blocking Iteration on Multi

For `Multi` streams, two methods convert the asynchronous stream into a blocking iterator or Java stream:

- `toBlockingIterable()`: returns an `Iterable` that blocks on each element
- `toBlockingStream()`: returns a `java.util.stream.Stream` that blocks on each element

=== "Mutiny Bindings"

    ```java
    AsyncFile asyncFile = vertx.fileSystem()
            .openAndAwait(path, new OpenOptions());

    asyncFile.toBlockingStream()
            .forEach(buffer::appendBuffer);
    ```

=== "Vanilla Vert.x"

    ```java
    AsyncFile asyncFile = vertx.fileSystem()
            .open(path, new OpenOptions())
            .toCompletionStage().toCompletableFuture().join();

    asyncFile.handler(chunk -> buffer.appendBuffer(chunk));
    ```

These are useful for consuming a reactive stream in imperative code, such as reading a file or draining a message consumer.

## JUnit 5 Testing Pattern

A standard pattern for testing Mutiny Vert.x code with JUnit 5:

=== "Mutiny Bindings"

    ```java
    class WebClientTest {

        Vertx vertx;

        @BeforeEach
        void setUp() {
            vertx = Vertx.vertx();
        }

        @AfterEach
        void tearDown() {
            vertx.closeAndAwait();
        }

        @Test
        void getJsonObject() {
            // ... set up a test HTTP server ...

            WebClient client = WebClient.create(vertx, new WebClientOptions()
                    .setDefaultPort(server.actualPort())
                    .setDefaultHost("localhost"));

            HttpResponse<JsonObject> response = client.get("/yo")
                    .as(BodyCodec.jsonObject())
                    .sendAndAwait();

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body().getString("msg")).isEqualTo("hello");
        }
    }
    ```

=== "Vanilla Vert.x"

    ```java
    @ExtendWith(VertxExtension.class)
    class WebClientTest {

        @Test
        void getJsonObject(Vertx vertx, VertxTestContext testContext) {
            // ... set up a test HTTP server ...

            WebClient client = WebClient.create(vertx, new WebClientOptions()
                    .setDefaultPort(server.actualPort())
                    .setDefaultHost("localhost"));

            client.get("/yo")
                    .as(BodyCodec.jsonObject())
                    .send()
                    .onComplete(testContext.succeeding(response -> {
                        testContext.verify(() -> {
                            assertThat(response.statusCode()).isEqualTo(200);
                            assertThat(response.body().getString("msg")).isEqualTo("hello");
                        });
                        testContext.completeNow();
                    }));
        }
    }
    ```

Key points:

- Create a fresh `Vertx` instance in `@BeforeEach` to isolate tests.
- Shut it down with `closeAndAwait()` in `@AfterEach` to release resources.
- Use `andAwait()` or `.await().atMost()` in the test body: JUnit test threads are not event loop threads, so blocking is safe.

With the Mutiny bindings, tests read sequentially with no callbacks. With vanilla Vert.x, you need `VertxExtension`, `VertxTestContext`, and nested `succeeding` / `verify` callbacks to handle the asynchronous result.

## executeBlocking for CPU-Intensive Work

When you need to run CPU-intensive or legacy blocking code from within a Vert.x context, use `vertx.executeBlocking()`. This offloads work to a worker thread pool, returning a result asynchronously.

=== "Mutiny Bindings"

    ```java
    Uni<String> uni = vertx.executeBlocking(() -> {
        // Runs on a worker thread — blocking calls are safe here
        return expensiveComputation();
    });

    String result = uni.await().indefinitely();
    ```

=== "Vanilla Vert.x"

    ```java
    Future<String> future = vertx.executeBlocking(() -> {
        // Runs on a worker thread — blocking calls are safe here
        return expensiveComputation();
    });

    String result = future.toCompletionStage().toCompletableFuture().join();
    ```

Inside the `executeBlocking` callable, you are on a worker thread, so blocking calls are safe. The Mutiny variant returns a `Uni` that can be composed into a larger reactive pipeline or awaited directly.
