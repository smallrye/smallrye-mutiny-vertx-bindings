#!/usr/bin/env just --justfile

# Build everything
build:
    ./mvnw clean install

# Build everything (no tests)
build-fast:
    ./mvnw -DskipTests clean install -T4

# Build the generator
build-generator:
    ./mvnw clean install -f vertx-mutiny-code-generator

# Build the generator
build-generator-fast:
    ./mvnw clean install -DskipTests -f vertx-mutiny-code-generator

# Compatibility report analysis
compatibility-report:
    ./mvnw verify -DskipTests revapi:report@revapi-check  -Prevapi -DskipTests -Dmaven.javadoc.skip=true -pl \!vertx-mutiny-clients-bom -pl \!vertx-mutiny-clients/vertx-mutiny-sql-client
    jbang CompatibilityReport.java && asciidoctor target/compatibility-report.adoc
