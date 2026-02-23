# Type Mapping

This page documents how Vert.x types are converted into Mutiny types by the code generator.
Understanding these mappings is essential for working with the generated bindings.

## Overview

The code generator parses Vert.x source code and produces Mutiny shim classes that wrap the original Vert.x APIs.
The core idea is to replace callback- and `Future`-based patterns with `Uni` and `Multi` from Mutiny.
Types that are already plain data (`JsonObject`, primitives, enums, data objects) pass through unchanged.

## Type mapping table

| Vert.x Type | Mutiny Type | Notes |
|---|---|---|
| `Future<T>` | `Uni<T>` | Primary async conversion |
| `ReadStream<T>` | `Multi<T>` | Via `toMulti()` on the shim |
| `WriteStream<T>` | `Flow.Subscriber<T>` | Via `toSubscriber()` on the shim |
| `Handler<AsyncResult<T>>` | _(removed)_ | Replaced by subscription model |
| `Handler<T>` | `Consumer<T>` | Standard functional interface |
| `Handler<Void>` | `Runnable` | No argument needed |
| `Handler<Promise<T>>` | `Uni<T>` | Promise-based handlers become Unis |
| `Consumer<Promise<T>>` | `Uni<T>` | Same treatment as Handler of Promise |
| `@VertxGen` types | Shim in `io.vertx.mutiny.*` | Each interface gets a wrapper class |
| `JsonObject`, `JsonArray` | Pass-through | Used as-is, no conversion |
| Primitives, boxed types | Pass-through | `int`, `Integer`, `boolean`, etc. |
| Data objects | Pass-through | Vert.x data objects are used directly |
| Enums | Pass-through | Enum types are used directly |
| `String` | Pass-through | Used as-is |
| `Throwable` | Pass-through | Used as-is |
| `List<T>`, `Set<T>` | Same collection, elements converted | If `T` is a `@VertxGen` type, each element is wrapped |
| `Map<K, V>` | Same map, values converted | If `V` is a `@VertxGen` type, each value is wrapped |

## Shim classes

Every Vert.x interface annotated with `@VertxGen` gets a corresponding **shim class** generated in the `io.vertx.mutiny.*` package.
The shim wraps the original Vert.x object (called the **delegate**) and exposes converted method signatures.

Key properties of shim classes:

- They implement `MutinyDelegate`, which provides the `getDelegate()` method.
- They live in `io.vertx.mutiny.*`, mirroring the original package structure (e.g. `io.vertx.core.eventbus.EventBus` becomes `io.vertx.mutiny.core.eventbus.EventBus`).
- They have a `newInstance()` static factory method used internally for wrapping delegates.
- Type parameters on the original interface are preserved in the shim.

```java
// Original Vert.x interface
io.vertx.core.eventbus.EventBus

// Generated Mutiny shim
io.vertx.mutiny.core.eventbus.EventBus

// Accessing the underlying delegate
io.vertx.core.eventbus.EventBus delegate = mutinyEventBus.getDelegate();
```

## Generated method variants

For every Vert.x method that returns a `Future<T>`, the generator produces three method variants: the `Uni`-returning reactive method, a blocking `AndAwait` method, and a fire-and-forget `AndForget` method.

See [Uni and Multi — The three method variants](uni-and-multi.md#the-three-method-variants) for the full table, return types, and guidance on when to use each variant.

## ReadStream conversion

When a `@VertxGen` interface extends `ReadStream<T>`, the generated shim gains three additional methods:

| Method | Return type | Description |
|---|---|---|
| `toMulti()` | `Multi<T>` | Converts the read stream into a Mutiny `Multi` |
| `toBlockingIterable()` | `Iterable<T>` | Returns a blocking iterable backed by the `Multi` |
| `toBlockingStream()` | `Stream<T>` | Returns a blocking Java `Stream` backed by the `Multi` |

The `toMulti()` method is lazy and cached: calling it multiple times returns the same `Multi` instance.
If `T` is itself a `@VertxGen` type, each emitted item is automatically wrapped in its shim class.

## WriteStream conversion

When a `@VertxGen` interface extends `WriteStream<T>`, the generated shim gains a `toSubscriber()` method:

| Method | Return type | Description |
|---|---|---|
| `toSubscriber()` | `Flow.Subscriber<T>` | Converts the write stream into a reactive `Flow.Subscriber` |

This lets you pipe a `Multi` into a Vert.x `WriteStream` using standard reactive streams back-pressure.

## Collections of @VertxGen types

When a method returns `Future<List<X>>`, `Future<Set<X>>`, or `Future<Map<K, V>>` where `X` or `V` is a `@VertxGen` type, the generator automatically wraps each element in its shim class.

For example, a method returning `Future<List<io.vertx.core.eventbus.Message<T>>>` becomes `Uni<List<io.vertx.mutiny.core.eventbus.Message<T>>>`, and each `Message` in the list is wrapped in the Mutiny shim.

The same applies to `Set` (using `Collectors.toSet()`) and `Map` values (using `Collectors.toMap()`).
