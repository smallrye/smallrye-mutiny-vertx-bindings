# SmallRye Reactive Utilities

[![Build Status](https://semaphoreci.com/api/v1/smallrye/smallrye-reactive-utils/branches/master/badge.svg)](https://semaphoreci.com/smallrye/smallrye-reactive-utils)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=smallrye_smallrye-reactive-utils&metric=alert_status)](https://sonarcloud.io/dashboard?id=smallrye_smallrye-reactive-utils)

This contains a set of modules helping the development of reactive applications in SmallRye 

## Vert.x Mutiny Clients

[Mutiny](https://smallrye.io/smallrye-mutiny) is a novel approach to deal with Reactive APIs.
The `vertx-mutiny-clients` module contains the Vert.x client API using the Mutiny model (`Uni` and `Multi`).

It also contains the Vert.x code generator.

## Vert.x Axle Clients - deprecated

This module delivers Vert.x client using the _Axle_ API. 

The _Axle_ API is based on `CompletionStage` and `Publisher`.

## Build

`mvn clean install`

## Release

`mvn release:prepare release:perform`
