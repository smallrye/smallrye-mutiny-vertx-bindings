# Vert.x Mutiny bindings generator migration guide

## Overview

With the release of Vert.x 5, important API changes were made, sometimes breaking the compability with Vert.x 4.x projects. It was an opportunity to release a new version of the generator. This guide will walk through the changes between the Mutiny Vert.x bindings for Vert.x 4 and Vert.x 5.

## A new Mutiny bindings generator
In its 3.x version, the Vert.x-mutiny bindings generator was an annotation processor. It was executed on a given module by using the `maven-processor-plugin`. 
As an annotation processor, it can only be run during maven compilation. Generating bindings thus requires compiling or recompiling the sources. 
The new approach still uses annotations to detect which classes and interfaces must be generated, but annotation processing is now part of a full CLI possessing its own `Main` class and its own lifecycle independent from that of the sources.
This makes it easier to test and faster to run.

## API changes
Numerous API changes are due to the transition to Vert.x 5.
The Vert.x 4 to 5 migration guide is available at https://vertx.io/docs/guides/vertx-5-migration-guide/.

We provide here a brief summary of the breaking changes.

### No more Callbacks

Complete removal of callback methods such as:
 ```java
void request(RequestOptions request, Handler<AsyncResult<HttpClientRequest>> callback);
 ```

They have all been replaced with a Future such as:
```java
Future<HttpClientRequest> request(RequestOptions request);
```

Consequently, the bindings generator only supports the `Future` style and will generate the following:
```java
Uni<HttpClientRequest> request(RequestOptions request);
```

### Deprecated modules are no more
| Sunsetting in 4.x     | Replacement in 5.x                                                                |
|-----------------------|-----------------------------------------------------------------------------------|
| Vert.x Sync           | Vert.x virtual threads                                                            |
| Service Factories     | None                                                                              |
| Maven Service Factory | None                                                                              |
| HTTP Service Factory  | None                                                                              |
| Vert.x Web OpenAPI    | [Vert.x Web OpenAPI Router](https://vertx.io/docs/vertx-web-openapi-router/java/) |
| Vert.x CLI            | None                                                                              |


### Changes with regards to `@DataObject` and `@VertxGen`
Numerous classes are now annotated `@DataObject` instead of  `@VertxGen`.  `@DataObject` annotation existed in Vert.x 4.x and was used mainly for `*Options.java` classes.    No wrappers are thus required, the core Vert.x interface is used directly. They will break existing projects are imports are now invalid.

Below are some classes that are not converted to Mutiny shims anymore: 

- `io.vertx.core.buffer.Buffer`
- `io.vertx.core.net.Address`
- `io.vertx.core.net.SocketAddress`
- `io.vertx.core.parsetools.JsonEvent`
- `io.vertx.core.MultiMap`
- `io.vertx.core.datagram.DatagramPacket`

### SQL clients have been reworked
In Vert.x 4, `Pool` subtypes were used for every kind of databases: `PgPool, MySQLPool, MSSQLPool`, etc. They have all been removed and replaced with client builder subtypes:

```java
// 4.x
PgPool client = PgPool.pool(vertx, connectOptions, poolOptions);

//5.0
Pool client = PgBuilder.pool()
  .with(poolOptions)
  .connectingTo(connectOptions)
  .using(vertx)
  .build();
```

For more information, see [Vert.x 5 migration guide](https://vertx.io/docs/guides/vertx-5-migration-guide/#_vert_x_sql_client). 

## Dropped or replaced modules

| Module              | Replacement                   |
| ------------------- | ----------------------------- |
| service-discovery-* | *dropped*                     |
| web-openapi         | (openapi, web-openapi-router) |
| web-auth-shiro      | *dropped*                     |
| web-auth-webauthn   | web-auth-webauthn4j           |
| auth-jdbc           | *dropped*                     |
| jdbc-client         | *dropped*                     |
| web-api-contract    | *dropped*                     |
| web-templ-jade      | *dropped*                     |
| stomp               | *dropped*                     |
| shell               | *dropped*                     |


## Using the generator with your own asynchronous APIs
[Learn how to use the bindings generator with your own asynchronous APIs](using-the-generator.md).