# Available Modules

This project provides Mutiny bindings for a wide range of Vert.x modules: web, databases, messaging, authentication, templating, and more.

## Naming Convention

All modules share the same group ID and follow a predictable artifact ID pattern:

- **Group ID**: `io.smallrye.reactive`
- **Artifact ID**: `smallrye-mutiny-vertx-{name}`

The `{name}` suffix mirrors the Vert.x module name.
For example, the Vert.x `vertx-web-client` module becomes `smallrye-mutiny-vertx-web-client`.

## Version Management

Import the BOM to manage versions centrally.
See [Getting Started — Adding the BOM](getting-started.md#adding-the-bom) for the dependency management snippet.

## Finding the Right Module

To find the Mutiny binding for a Vert.x module, take the Vert.x artifact ID and prefix it with `smallrye-mutiny-`.
For example, the Vert.x `vertx-pg-client` module becomes `smallrye-mutiny-vertx-pg-client`:

```xml
<dependency>
    <groupId>io.smallrye.reactive</groupId>
    <artifactId>smallrye-mutiny-vertx-pg-client</artifactId>
</dependency>
```

This pattern applies consistently across all modules.

## Imports

When using a Mutiny binding, import from the `io.vertx.mutiny.*` packages rather than `io.vertx.core.*` or `io.vertx.ext.*`.
The Mutiny classes are generated shims that wrap the original Vert.x types.

```java
// Correct: Mutiny shim
import io.vertx.mutiny.ext.web.client.WebClient;

// Wrong: original Vert.x type
import io.vertx.ext.web.client.WebClient;
```

Configuration and options classes (e.g., `WebClientOptions`) are data objects and are used directly from the Vert.x packages since they do not need shims.
