# Error Handling

One advantage of using Mutiny with Vert.x is that errors propagate through the reactive pipeline automatically.
With the callback or `Future`-based API you must manually check for failures at each step.
With Mutiny, failures flow downstream until something handles them, so you focus on the success path and deal with errors once:

=== "Mutiny Bindings"

    ```java
    // Errors propagate automatically
    client.request(HttpMethod.GET, "/data")
        .onItem().transformToUni(HttpClientRequest::send)
        .onItem().transformToUni(HttpClientResponse::body)
        .subscribe().with(
            body -> System.out.println("Got: " + body),
            failure -> System.err.println("Failed: " + failure.getMessage())
        );
    ```

=== "Vanilla Vert.x"

    ```java
    // Manual failure checks at each step
    client.request(HttpMethod.GET, "/data")
        .compose(request -> request.send())
        .compose(response -> response.body())
        .onSuccess(body -> System.out.println("Got: " + body))
        .onFailure(err -> System.err.println("Failed: " + err.getMessage()));
    ```

If any step in the pipeline fails, the remaining `onItem()` operators are skipped and the failure handler receives the error. With vanilla Vert.x, `compose()` chains behave similarly — a failure in any stage short-circuits to the `onFailure` handler — but the Mutiny API makes the distinction between item and failure handling more explicit through its operator vocabulary.

## Basic failure handling

The simplest way to handle errors is to provide both a success and failure callback to `subscribe().with()`:

=== "Mutiny Bindings"

    ```java
    uni.subscribe().with(
        item -> System.out.println("Received: " + item),
        failure -> System.err.println("Error: " + failure.getMessage())
    );
    ```

=== "Vanilla Vert.x"

    ```java
    future
        .onSuccess(item -> System.out.println("Received: " + item))
        .onFailure(err -> System.err.println("Error: " + err.getMessage()));
    ```

If you omit the failure callback, unhandled failures are reported to Mutiny's global failure handler.

## Recovery

You can recover from failures inline rather than letting them propagate to the subscriber.

### Recover with a fallback item

=== "Mutiny Bindings"

    ```java
    Uni<String> result = uni
        .onFailure().recoverWithItem("default value");
    ```

=== "Vanilla Vert.x"

    ```java
    Future<String> result = future
        .recover(err -> Future.succeededFuture("default value"));
    ```

### Recover with another Uni

When recovery itself requires an asynchronous operation, use `recoverWithUni`:

=== "Mutiny Bindings"

    ```java
    Uni<String> result = uni
        .onFailure().recoverWithUni(() -> fetchFromFallbackService());
    ```

=== "Vanilla Vert.x"

    ```java
    Future<String> result = future
        .recover(err -> fetchFromFallbackService());
    ```

### Recover from specific exceptions

Both recovery methods accept a predicate or exception class to recover selectively:

=== "Mutiny Bindings"

    ```java
    Uni<String> result = uni
        .onFailure(TimeoutException.class).recoverWithItem("timed out")
        .onFailure(IOException.class).recoverWithUni(() -> retryElsewhere());
    ```

=== "Vanilla Vert.x"

    ```java
    Future<String> result = future
        .recover(err -> {
            if (err instanceof TimeoutException) {
                return Future.succeededFuture("timed out");
            }
            if (err instanceof IOException) {
                return retryElsewhere();
            }
            return Future.failedFuture(err);
        });
    ```

With Mutiny, selective recovery is declarative — you chain `.onFailure(ExceptionType.class)` for each case. With vanilla Vert.x, you use a single `recover()` block and check types with `instanceof`.

## Retries

Mutiny provides built-in retry support:

=== "Mutiny Bindings"

    ```java
    Uni<String> result = uni
        .onFailure().retry().atMost(3);
    ```

=== "Vanilla Vert.x"

    ```java
    Future<String> fetchWithRetry(int retries) {
        return doFetch()
            .recover(err -> {
                if (retries > 0) {
                    return fetchWithRetry(retries - 1);
                }
                return Future.failedFuture(err);
            });
    }

    Future<String> result = fetchWithRetry(3);
    ```

This retries the upstream operation up to 3 times on any failure.
With vanilla Vert.x, retries require a recursive method that calls itself from within a `recover()` block.

For production use, add exponential backoff to avoid overwhelming a failing service:

=== "Mutiny Bindings"

    ```java
    Uni<String> result = uni
        .onFailure().retry()
            .withBackOff(Duration.ofMillis(100), Duration.ofSeconds(5))
            .atMost(5);
    ```

=== "Vanilla Vert.x"

    ```java
    Future<String> fetchWithBackoff(int retries, long delayMs) {
        return doFetch()
            .recover(err -> {
                if (retries > 0) {
                    Promise<String> promise = Promise.promise();
                    long nextDelay = Math.min(delayMs * 2, 5000);
                    vertx.setTimer(delayMs, id ->
                        fetchWithBackoff(retries - 1, nextDelay)
                            .onComplete(promise));
                    return promise.future();
                }
                return Future.failedFuture(err);
            });
    }

    Future<String> result = fetchWithBackoff(5, 100);
    ```

This retries up to 5 times with an initial delay of 100ms that increases exponentially up to a maximum of 5 seconds between attempts.
With vanilla Vert.x, exponential backoff requires managing a `Promise`, a timer via `vertx.setTimer()`, and manual delay calculation — all of which Mutiny handles as a single operator chain.

## Failure transformation

You can transform a failure into a different exception type, which is useful for wrapping low-level errors into domain-specific ones:

=== "Mutiny Bindings"

    ```java
    Uni<String> result = uni
        .onFailure().transform(err -> new MyServiceException("Operation failed", err));
    ```

=== "Vanilla Vert.x"

    ```java
    Future<String> result = future
        .recover(err -> Future.failedFuture(
            new MyServiceException("Operation failed", err)));
    ```

## Vert.x expectations with Mutiny

Vert.x provides an `Expectation` interface that works as a predicate on resolved `Future` values.
Pre-defined expectations such as `HttpResponseExpectation.status(200)` are convenient for validating HTTP responses.

Since the Mutiny bindings convert `Future`-returning methods into `Uni`-returning methods, you cannot use expectations directly.
Instead, the `io.smallrye.mutiny.vertx.core.Expectations` helper class adapts them for use with `Uni.plug()`.

### Simple expectation

=== "Mutiny Bindings"

    ```java
    import static io.smallrye.mutiny.vertx.core.Expectations.expectation;

    Expectation<Integer> tenToTwenty = (value -> value >= 10 && value <= 20);

    Uni.createFrom().item(15)
        .plug(expectation(tenToTwenty));
    ```

=== "Vanilla Vert.x"

    ```java
    Expectation<Integer> tenToTwenty = (value -> value >= 10 && value <= 20);

    Future.succeededFuture(15)
        .expecting(tenToTwenty);
    ```

If the value does not satisfy the expectation, the `Uni` fails (or the `Future` fails in vanilla Vert.x).
For example, passing `42` instead of `15` would produce a failure with the message: `Unexpected result: 42`.

### HTTP response expectations

Vert.x ships pre-defined expectations for HTTP responses, such as `status()` and `contentType()`.
These expectations are typed against core Vert.x types (e.g., `io.vertx.core.http.HttpResponseHead`), not against the Mutiny shim types (e.g., `io.vertx.mutiny.core.http.HttpResponseHead`).

To bridge this gap, the `expectation` helper accepts an extractor function as its first argument.
Passing `HttpClientResponse::getDelegate` unwraps the Mutiny shim so the expectation receives the underlying Vert.x type:

=== "Mutiny Bindings"

    ```java
    import static io.smallrye.mutiny.vertx.core.Expectations.expectation;
    import static io.vertx.core.http.HttpResponseExpectation.contentType;
    import static io.vertx.core.http.HttpResponseExpectation.status;

    vertx.createHttpClient()
        .request(HttpMethod.GET, port, "localhost", "/")
        .onItem().transformToUni(HttpClientRequest::send)
        .plug(expectation(HttpClientResponse::getDelegate, status(200).and(contentType("text/plain"))))
        .onItem().transformToUni(HttpClientResponse::body)
        .await().atMost(Duration.ofSeconds(5));
    ```

=== "Vanilla Vert.x"

    ```java
    import static io.vertx.core.http.HttpResponseExpectation.contentType;
    import static io.vertx.core.http.HttpResponseExpectation.status;

    vertx.createHttpClient()
        .request(HttpMethod.GET, port, "localhost", "/")
        .compose(HttpClientRequest::send)
        .expecting(status(200).and(contentType("text/plain")))
        .compose(HttpClientResponse::body)
        .toCompletionStage().toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
    ```

In this example:

- `HttpClientResponse::getDelegate` extracts the core Vert.x response from the Mutiny shim. With vanilla Vert.x, no unwrapping is needed since you work directly with the core types.
- `status(200).and(contentType("text/plain"))` combines two expectations: the response must have status 200 and content type `text/plain`.
- If either expectation fails, the `Uni` pipeline (or the `Future` chain) fails with a descriptive error.

You can combine any number of expectations using `.and()`.
