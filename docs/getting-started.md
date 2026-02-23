# Getting Started

This guide walks you through adding SmallRye Mutiny Vert.x Bindings to your project and writing a first reactive application.

## Prerequisites

- **Java 17** or later
- **Maven 3.9** or later

## Adding the BOM

The simplest way to manage dependency versions is to import the BOM (Bill of Materials) in your `pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.smallrye.reactive</groupId>
            <artifactId>vertx-mutiny-clients-bom</artifactId>
            <version>${mutiny-vertx.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Replace `${mutiny-vertx.version}` with the version you want to use, or define it as a Maven property.

## Adding module dependencies

With the BOM imported you can add individual modules without specifying a version.
At a minimum you need the core module:

```xml
<dependency>
    <groupId>io.smallrye.reactive</groupId>
    <artifactId>smallrye-mutiny-vertx-core</artifactId>
</dependency>
```

If you also want to use the reactive HTTP client (WebClient), add:

```xml
<dependency>
    <groupId>io.smallrye.reactive</groupId>
    <artifactId>smallrye-mutiny-vertx-web-client</artifactId>
</dependency>
```

## A complete example

The following program starts an HTTP server that replies with a greeting and then uses WebClient to call it:

=== "Mutiny Bindings"

    ```java
    import io.vertx.mutiny.core.Vertx;
    import io.vertx.mutiny.core.http.HttpServer;
    import io.vertx.mutiny.ext.web.client.WebClient;
    import io.vertx.ext.web.client.WebClientOptions;
    import io.vertx.mutiny.ext.web.client.HttpResponse;
    import io.vertx.mutiny.ext.web.codec.BodyCodec;

    public class GettingStarted {

        public static void main(String[] args) {
            Vertx vertx = Vertx.vertx();

            HttpServer server = vertx.createHttpServer()
                    .requestHandler(req -> req.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "text/plain")
                            .endAndForget("Hello from Mutiny!"))
                    .listenAndAwait(8080);

            System.out.println("Server listening on port " + server.actualPort());

            WebClient client = WebClient.create(vertx, new WebClientOptions()
                    .setDefaultPort(8080)
                    .setDefaultHost("localhost"));

            HttpResponse<String> response = client.get("/")
                    .as(BodyCodec.string())
                    .sendAndAwait();

            System.out.println("Response: " + response.body());

            vertx.closeAndAwait();
        }
    }
    ```

=== "Vanilla Vert.x"

    ```java
    import io.vertx.core.Vertx;
    import io.vertx.ext.web.client.WebClient;
    import io.vertx.ext.web.client.WebClientOptions;
    import io.vertx.ext.web.codec.BodyCodec;

    public class GettingStarted {

        public static void main(String[] args) {
            Vertx vertx = Vertx.vertx();

            vertx.createHttpServer()
                    .requestHandler(req -> req.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "text/plain")
                            .end("Hello from Vert.x!"))
                    .listen(8080)
                    .compose(server -> {
                        System.out.println("Server listening on port " + server.actualPort());

                        WebClient client = WebClient.create(vertx, new WebClientOptions()
                                .setDefaultPort(8080)
                                .setDefaultHost("localhost"));

                        return client.get("/")
                                .as(BodyCodec.string())
                                .send();
                    })
                    .onSuccess(response ->
                            System.out.println("Response: " + response.body()))
                    .onComplete(v -> vertx.close());
        }
    }
    ```

### What is happening here

1. `Vertx.vertx()` creates a Mutiny-based Vert.x instance.
2. `listenAndAwait(8080)` starts the server and blocks the calling thread until the server is ready. Under the hood it subscribes to the `Uni<HttpServer>` returned by `listen()` and awaits its result.
3. `sendAndAwait()` sends the HTTP request and blocks until the response arrives: again a convenience wrapper around a `Uni`.
4. `closeAndAwait()` shuts down the Vert.x instance.

### Using explicit reactive operators

The `*AndAwait()` helpers are convenient for scripts and tests, but in production code you will typically compose operations with Mutiny operators.
Here is the same WebClient call expressed reactively:

=== "Mutiny Bindings"

    ```java
    client.get("/")
            .as(BodyCodec.string())
            .send()
            .onItem().transform(HttpResponse::body)
            .subscribe().with(
                    body -> System.out.println("Response: " + body),
                    failure -> System.err.println("Request failed: " + failure.getMessage())
            );
    ```

=== "Vanilla Vert.x"

    ```java
    client.get("/")
            .as(BodyCodec.string())
            .send()
            .map(HttpResponse::body)
            .onSuccess(body -> System.out.println("Response: " + body))
            .onFailure(err -> System.err.println("Request failed: " + err.getMessage()));
    ```

Note the use of `onItem().transform()` rather than shorthand aliases: this project favours explicit operator names so that the pipeline reads clearly.

!!! tip "Use `io.vertx.mutiny.*` imports"

    Always import classes from the `io.vertx.mutiny.*` packages, **not** from `io.vertx.core.*`.
    The Mutiny classes are generated shims that wrap the Vert.x core classes and expose `Uni` / `Multi` return types instead of `Future`.

## Next steps

- [Uni and Multi](uni-and-multi.md): learn how `Future` and `ReadStream` map to Mutiny types.
- [Type Mapping](type-mapping.md): understand how Vert.x types are converted in the generated bindings.
- [Error Handling](error-handling.md): handle failures and use Vert.x expectations with Mutiny.
- [Available Modules](available-modules.md): see the full list of generated client modules.
