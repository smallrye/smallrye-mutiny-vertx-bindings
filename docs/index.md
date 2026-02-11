# Overview

SmallRye Mutiny Vert.x Bindings provides Vert.x client APIs using the [Mutiny](https://smallrye.io/smallrye-mutiny/) reactive programming model.
Instead of working with Vert.x `Future` and callback-based APIs, you use `Uni` (for single results) and `Multi` (for streams of items) with a rich, explicit set of operators.
The bindings are generated automatically from the Vert.x source code, so they stay in sync with every Vert.x release.

## Why Mutiny?

- **Lazy evaluation**: a `Uni` does nothing until you subscribe, giving you full control over when side effects happen.
- **Explicit operator vocabulary**: methods like `onItem().transform()` and `onFailure().retry()` make the intent of each pipeline stage obvious.
- **Back-pressured streams**: `Multi` implements Reactive Streams, so producers cannot overwhelm consumers.
- **Blocking helpers for tests and virtual threads**: call `andAwait()` to block until a result is available, which is useful in unit tests and when running on virtual threads.

## Quick Example

The following example fetches JSON data from an HTTP endpoint. Compare the Mutiny bindings with the equivalent vanilla Vert.x code:

=== "Mutiny Bindings"

    ```java
    Uni<JsonObject> fetchData(WebClient client) {
        return client.get("/api/data")
            .as(BodyCodec.jsonObject())
            .send()
            .onItem().transform(HttpResponse::body)
            .onFailure().retry().atMost(3)
            .onFailure().invoke(err -> logger.error("Request failed", err));
    }
    ```

=== "Vanilla Vert.x"

    ```java
    Future<JsonObject> fetchData(WebClient client, int retries) {
        return client.get("/api/data")
            .as(BodyCodec.jsonObject())
            .send()
            .map(HttpResponse::body)
            .recover(err -> {
                logger.error("Request failed", err);
                if (retries > 0) {
                    return fetchData(client, retries - 1);
                }
                return Future.failedFuture(err);
            });
    }
    ```

With the Mutiny bindings, every step in the pipeline is visible:

1. `send()` returns a `Uni<HttpResponse<JsonObject>>`: nothing happens yet.
2. `onItem().transform()` maps the response to its JSON body when the item arrives.
3. `onFailure().retry().atMost(3)` retries the entire operation up to 3 times on failure.
4. `onFailure().invoke()` logs errors that persist after retries, without swallowing them.

Notice that retry logic requires no extra code with Mutiny — it is a built-in operator. With vanilla Vert.x, you need to manage a retry counter and use recursive `recover()` calls, as shown in the second tab.

You can also block for the result when that is appropriate (tests, virtual threads):

```java
JsonObject body = fetchData(client).await().indefinitely();
```

!!! tip "Explicit operators"

    Mutiny favours explicit names over short aliases.
    Prefer `onItem().transform()` over `map`, `onItem().transformToUni()` over `flatMap`, and `onItem().invoke()` over a bare `invoke`.
    The longer forms make pipelines easier to read and review.

## Next Steps

- [Getting Started](getting-started.md): add the dependency and write your first Mutiny Vert.x program.
- [Uni and Multi](uni-and-multi.md): learn the two core reactive types in depth.
- [Type Mapping](type-mapping.md): understand how Vert.x types are converted to Mutiny types.
- [Error Handling](error-handling.md): strategies for handling failures in reactive pipelines.
- [Available Modules](available-modules.md): the full list of generated client modules.
