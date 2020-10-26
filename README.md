# SmallRye Reactive Utilities

[![Build Status](https://github.com/smallrye/smallrye-reactive-utils/workflows/SmallRye%20Build/badge.svg?branch=master)]( https://github.com/smallrye/smallrye-reactive-utils/actions?query=workflow%3A%22SmallRye+Build%22)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=smallrye_smallrye-reactive-utils&metric=alert_status)](https://sonarcloud.io/dashboard?id=smallrye_smallrye-reactive-utils)
[![License](https://img.shields.io/github/license/smallrye/smallrye-fault-tolerance.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven](https://img.shields.io/maven-central/v/io.smallrye.reactive/smallrye-reactive-utilities-projects?color=green)]()

This contains a set of modules helping the development of reactive applications in SmallRye 

## Vert.x Mutiny Clients

[Mutiny](https://smallrye.io/smallrye-mutiny) is a novel approach to deal with Reactive APIs.
The `vertx-mutiny-clients` module contains the Vert.x client API using the Mutiny model (`Uni` and `Multi`).

It also contains the Vert.x code generator.

## Reactive Converters

Reactive converters are a set of library to convert types uses by various libraries from/to `Publisher` and `CompletionStage`.
Documentation is available in [the reactive-converters directory](./reactive-converters/readme.adoc).

## Build

`mvn clean install`

## Release

`mvn release:prepare release:perform`
