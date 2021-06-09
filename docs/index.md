# Overview

[Eclipse Vert.x](https://vertx.io/) is the leading toolkit for writing reactive applications on the JVM.

While the Vert.x core APIs expose asynchronous programming through _callbacks_ and _promise / future_, code generators offer bindings to other asynchronous programming models, including: [Kotlin coroutines](https://github.com/vert-x3/vertx-lang-kotlin/tree/master/vertx-lang-kotlin-coroutines), and [RxJava 1, 2 and 3](https://github.com/vert-x3/vertx-rx).

This project offers [Vert.x](https://vertx.io/) binding for [Mutiny, an intuitive event-driven reactive programming library for Java](https://smallrye.io/smallrye-mutiny/).

## Getting the bindings

The bindings can be accessed from the following Maven coordinates:

* Group: `io.smallrye.reactive`
* Artifact: `smallrye-mutiny-vertx-<MODULE>` where `MODULE` refers to a Vert.x module, such as `core`, `pg-client`, `web-client`, etc.

!!! note "The Mutiny bindings are modular"
    If you are familiar with other Vert.x bindings such as those for RxJava then you need to be aware that the Mutiny bindings are offered on a per-module basis.
    For instance the RxJava 3 bindings are exposed through the `io.vertx:vertx-rx-java3` dependency, and that `vertx-rx-java3` has optional dependencies _on the whole Vert.x stack_.
    We think that it is cleaner to offer bindings on a per-module basis, so your project does not have optional dependencies on modules of the Vert.x stack that you don't consume.

The full list of supported modules from the Vert.x stack is available at [https://github.com/smallrye/smallrye-mutiny-vertx-bindings/tree/main/vertx-mutiny-clients](https://github.com/smallrye/smallrye-mutiny-vertx-bindings/tree/main/vertx-mutiny-clients)

## A short example

The following self-contained [JBang](https://www.jbang.dev) script shows some of the features of the Vert.x Mutiny bindings (see the highlights):

```java linenums="1" hl_lines="20 27 37 42 53"
--8<-- "docs/snippets/hello.java"
```

This script can be run with `./hello.java` or `jbang run hello.java`, and exposes a HTTP server on port 8080:

```
$ ./hello.java
[jbang] Building jar...
Deployment Starting
See http://127.0.0.1:8080
Deployment completed
```

The HTTP server responds to any HTTP request with the current value of a counter that is incremented every 2 seconds:

```
$ http :8080
HTTP/1.1 200 OK
content-length: 2

@1

$ http :8080
HTTP/1.1 200 OK
content-length: 2

@2

```

The deployed verticle uses the Mutiny API, where the `start(Promise<Void>)` method is replaced by `asyncStart()` method that returns a `Uni<Void>`.
The code also shows how to convert Vert.x streams into Mutiny `Multi` streams, and how to await for the verticle deployment to complete.
