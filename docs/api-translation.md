# API translation

The Vert.x Mutiny bindings are generated from existing APIs.

## Vert.x and asynchronous operation methods

Vert.x is a toolkit that simplifies asynchronous I/O programming.
The Vert.x APIs rely on simple conventions to denote methods doing asynchronous operations, as in:

```java
public void someOperation(Foo a, Bar b, Baz c, Handler<AsyncResult<T>> handler) {
    // (...)
}
```

Here `someOperation` is a method where `a`, `b` and `c` are _parameters_, and `handler` is a _callback_:

* the callback is notified when the asynchronous operation done in `someOperation` completes, and
* callbacks are always passed as a last parameter of type `Handler<AsyncResult<T>>`, and
* `AsyncResult` encapsulates an asynchronous result of type `T`, or a failure.

Since Vert.x 4 asynchronous operations can also be defined by returning a `Future<T>`, as in:

```java
public Future<T> someOperation(Foo a, Bar b, Baz c) {
    // (...)
}
```

Code generators (e.g., Mutiny, RxJava, Kotlin coroutines, etc) can spot these methods and derive their own [shim APIs](https://en.wikipedia.org/wiki/Shim_(computing)).

For instance with Mutiny `someOperation` becomes:

```java
public Uni<T> someOperation(Foo a, Bar b, Baz c) {
    // (...)
}
```

## What does the Mutiny code generator do?

![Mutiny code generator](images/codegen.png)

The code generator is applied to selected modules from the Vert.x stack.

The translation rules are the following.

| Rule                 | Translation                                      |
| -------------------- | ------------------------------------------------ |
| `io.vertx` package   | `io.vertx.mutiny` package                        |
| Asynchronous method  | A method returning a `Uni<T>`                    |
| `ReadStreams<T>`     | Consumed as a `Multi<T>`                         |
| `WriteStreams<T>`    | Consumed as a _Reactive Streams_ `Subscriber<T>` |

Because Mutiny is a _Reactive Streams_ compliant implementation, the generated shims also adapt the Vert.x back-pressure protocol to that of _Reactive Streams_.

!!! note "Method erasure"
    The Mutiny code generator _erases_ methods.
    Given an asynchronous method `foo(Handler<AsyncResult<T>>)`, then it is not present in the generated shim as it is replaced with `Uni<T> foo()`.

## Helper methods

Given an asynchronous operation method, the Mutiny code generator also provides 2 further variants:

* `xAndAwait()` to block the caller thread until the outcome is received, or throw a `RuntimeException` when a failure arises, and
* `xAndForget()` to trigger the operation then discard the outcome.

Given the `someOperation` method above, the generator provides these 3 methods:

```java
// The canonical Mutiny method
public Uni<T> someOperation(Foo a, Bar b, Baz c) {
    // (...)
}

// Blocks, may throw a RuntimeException
public T someOperationAndAwait(Foo a, Bar b, Baz c) {
    // (...)
}

// Does not block, the result or failure is discarded
public void someOperationAndForget(Foo a, Bar b, Baz c) {
    // (...), 
}
```

## Generate Mutiny variants from your own APIs

Vert.x uses an open code generator, so you can generate Mutiny APIs for your own asynchronous interfaces.

The code generator is available from the `io.smallrye.reactive:vertx-mutiny-generator` artifact.
It works with the Vert.x `io.vertx:vertx-codegen` annotation processor.

You can generate bindings by having the  artifacts on the classpath for annotation processing at compilation time, or you may use other ways in your builds.
The code for this project shows how to generate the files using Maven plugins.
