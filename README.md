# SmallRye Mutiny Vert.x Bindings

[![Build Status](https://github.com/smallrye/smallrye-mutiny-vertx-bindings/workflows/SmallRye%20Build/badge.svg?branch=main)](https://github.com/smallrye/smallrye-mutiny-vertx-bindings/actions?query=workflow%3A%22SmallRye+Build%22)
[![License](https://img.shields.io/github/license/smallrye/smallrye-fault-tolerance.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven](https://img.shields.io/maven-central/v/io.smallrye.reactive/smallrye-mutiny-vertx-bindings-projects?color=green)]()

## Vert.x Mutiny Clients

[Mutiny](https://smallrye.io/smallrye-mutiny) is a novel approach to deal with Reactive APIs.
The `vertx-mutiny-clients` module contains the Vert.x client API using the Mutiny model (`Uni` and `Multi`).

It also contains the Vert.x code generator.

## Build

`mvn clean install`

## Release

- open a pull request updating the `.github/project.yml` file with the desired release version and next development version.
- once the pull request is merged, the release will be cut (tag, deployment...)

## Compatibility Report

To generate the compatibility report, you need:

* jbang - https://github.com/jbangdev/jbang
* asciidoctor - http://asciidoctor.org/

Generate the report with:

```bash
mvn verify -DskipTests revapi:report@revapi-check  -Prevapi -DskipTests -Dmaven.javadoc.skip=true -pl \!vertx-mutiny-clients-bom
jbang CompatibilityReport.java && asciidoctor target/compatibility-report.adoc
``` 

The HTML report is available in `target/compatibility-report.html`

