# Using Vert.x expectations

Vert.x `Future` support [expectations](https://javadoc.io/static/io.vertx/vertx-core/4.5.10/io/vertx/core/Expectation.html) as predicates on the resolved values.

A good example of pre-defined expectations are those from `HttpResponseExpectation`:

```java
Future<JsonObject> future = client
    .request(HttpMethod.GET, "some-uri")
    .compose(request -> request
        .send()
        .expecting(HttpResponseExpectation.SC_OK.and(HttpResponseExpectation.JSON))
    .compose(response -> response
        .body()
        .map(buffer -> buffer.toJsonObject())));
```

In this example the HTTP response is expected to be with status code 200 (`SC_OK`) and have the `application/json` content-type (`JSON`).

## Where are expectations gone in a Vert.x Mutiny bindings API?

While expectations are very useful, they apply to `Future` in most of the Vert.x APIs such as the core HTTP client.

Since the Mutiny bindings generator turns `Future`-returning methods into `Uni`-returning methods, you need to leverage another route to use them.

## Turning expectations into operators

The `io.smallrye.mutiny.vertx.core.Expectations` class that comes with the `smallrye-mutiny-vertx-core` artifact bring 2 helper methods.

The `expecting` methods build functions that can be used with the `Uni::plug` operator, as in:

```java
Expectation<Integer> tenToTwenty = (value -> value >= 10 && value <= 20);

return Uni.createFrom().item(15)
    .plug(expectation(tenToTwenty));
```

Some expectations such as those in `HttpResponseExpectation` apply to types from the core Vert.x APIs.
Since the Mutiny bindings generate shims (e.g., `io.vertx.mutiny.core.http.HttpResponseHead`), you need to extract the delegate type for the expectations to work on the correct type (e.g., `io.vertx.core.http.HttpResponseHead`):

```java
return vertx.createHttpClient()
    .request(HttpMethod.GET, port, "localhost", "/")
    .chain(HttpClientRequest::send)
    .plug(expectation(HttpClientResponse::getDelegate, status(200).and(contentType("text/plain"))))
    .onItem().transformToUni(HttpClientResponse::body)
```

The extractor function here is `HttpClientResponse::getDelegate`, so that the `status(200).and(contentType("text/plain"))` expectation applies to `io.vertx.core.http.HttpResponseHead` and not the `io.vertx.mutiny.core.http.HttpResponseHead` generated shim.
