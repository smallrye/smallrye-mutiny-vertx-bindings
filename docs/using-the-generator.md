# Using the Generator

The Mutiny code generator takes Vert.x source files and produces Mutiny shim classes that wrap the original Vert.x APIs with `Uni` and `Multi` return types. You point it at a directory of Vert.x sources, and it outputs ready-to-compile Java files in the `io.vertx.mutiny.*` package.

## CLI Options

| Option | Required | Description |
|---|---|---|
| `--source <path>` | Yes | Vert.x Java source directory |
| `--output <path>` | No | Output directory (dry run if omitted) |
| `--additional-source <path>` | No, repeatable | Additional sources for symbol resolution |
| `--module-name <name>` | No | Module name for the generated bindings |

## CLI Usage

```bash
java -cp generator.jar io.smallrye.mutiny.vertx.apigenerator.Main \
    --source=path/to/vertx-sources \
    --output=path/to/output \
    --module-name=vertx-web-client
```

If `--output` is omitted the generator performs a dry run, parsing and resolving types without writing any files.

## Maven Integration

Most users run the generator as part of a Maven build. The pattern uses three plugins executed during the `generate-sources` phase.

### 1. Unpack Vert.x Sources

The `maven-dependency-plugin` extracts Vert.x source JARs into `target/sources/java` so the generator can parse them.

```xml
<plugin>
    <artifactId>maven-dependency-plugin</artifactId>
    <configuration>
        <includeGroupIds>io.vertx</includeGroupIds>
        <includeArtifactIds>vertx-web-client</includeArtifactIds>
        <classifier>sources</classifier>
        <includeTypes>jar</includeTypes>
    </configuration>
    <executions>
        <execution>
            <id>unpack-java</id>
            <phase>generate-sources</phase>
            <goals><goal>unpack-dependencies</goal></goals>
            <configuration>
                <includes>io/vertx/**/*.java</includes>
                <excludes>**/impl/**/*.java</excludes>
                <outputDirectory>${project.build.directory}/sources/java</outputDirectory>
            </configuration>
        </execution>
        <!-- Optional: unpack additional sources for cross-module resolution -->
        <execution>
            <id>unpack-vertx-core</id>
            <phase>generate-sources</phase>
            <goals><goal>unpack-dependencies</goal></goals>
            <configuration>
                <includes>io/vertx/**/*.java</includes>
                <excludes>**/impl/**/*.java</excludes>
                <includeArtifactIds>vertx-core,vertx-auth-common</includeArtifactIds>
                <outputDirectory>${project.basedir}/target/additional-sources/vertx-core</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

!!! tip "Additional sources"
You might need to generate Mutiny bindings for modules `moduleA` and `moduleB`, where `moduleB` uses classes or interfaces from `moduleA`.
In this instance, you should add `moduleA` as additional sources to ensure that you are using the generated mutiny API for `moduleA` in `moduleB`, and not the original one. 
This is easily achievable by setting the `maven-dependency-plugin` for `moduleB` like below.
```xml
<plugin>
    <artifactId>maven-dependency-plugin</artifactId>
    <configuration>
        <includeGroupIds>org.acme</includeGroupIds>
        <includeArtifactIds>moduleB</includeArtifactIds>
        <classifier>sources</classifier>
        <includeTypes>jar</includeTypes>
    </configuration>
    <executions>
        <execution>
            <id>unpack-java</id>
            <phase>generate-sources</phase>
            <goals><goal>unpack-dependencies</goal></goals>
            <configuration>
                <includes>io/vertx/**/*.java</includes>
                <excludes>**/impl/**/*.java</excludes>
                <outputDirectory>${project.build.directory}/sources/java</outputDirectory>
            </configuration>
        </execution>
        <!-- Optional: unpack additional sources for cross-module resolution -->
        <execution>
            <id>unpack-vertx-core</id>
            <phase>generate-sources</phase>
            <goals><goal>unpack-dependencies</goal></goals>
            <configuration>
                <includes>io/vertx/**/*.java</includes>
                <excludes>**/impl/**/*.java</excludes>
                <includeArtifactIds>vertx-core,moduleA</includeArtifactIds>
                <outputDirectory>${project.basedir}/target/additional-sources/vertx-core</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 2. Run the Generator

The `exec-maven-plugin` invokes the generator with the unpacked sources as input.

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>generate</id>
            <phase>generate-sources</phase>
            <goals><goal>java</goal></goals>
            <configuration>
                <mainClass>io.smallrye.mutiny.vertx.apigenerator.Main</mainClass>
                <arguments>
                    <arg>--module-name=vertx-web-client</arg>
                    <arg>--source=${project.basedir}/target/sources</arg>
                    <arg>--output=${project.basedir}/target/generated-sources/codegen</arg>
                    <arg>--additional-source=${project.basedir}/target/additional-sources/vertx-core</arg>
                </arguments>
                <includePluginDependencies>true</includePluginDependencies>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.smallrye.reactive</groupId>
            <artifactId>vertx-mutiny-code-generator</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

### 3. Add Generated Sources to the Build

The `build-helper-maven-plugin` adds the generated output directory as a source root so Maven compiles it alongside handwritten code.

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>add-source</id>
            <phase>generate-sources</phase>
            <goals><goal>add-source</goal></goals>
            <configuration>
                <sources>
                    <source>target/generated-sources/codegen</source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Cross-Module Type Resolution

When a Vert.x module references types defined in another module (for example, `vertx-web-client` uses types from `vertx-core`), the generator needs access to those additional sources to resolve types correctly. Use `--additional-source` to provide them. This flag is repeatable: pass it once for each extra source tree:

```bash
java -cp generator.jar io.smallrye.mutiny.vertx.apigenerator.Main \
    --source=target/sources \
    --output=target/generated-sources/codegen \
    --additional-source=target/additional-sources/vertx-core \
    --additional-source=target/additional-sources/vertx-auth-common
```

In the Maven setup, this corresponds to unpacking the additional module sources into separate directories (as shown in the optional execution above) and passing them via `<arg>` elements.

## When You Need the Generator

Most application developers consume the pre-built Mutiny bindings as Maven dependencies and never run the generator directly. You need it if you are:

- **A project maintainer** rebuilding the bindings after a Vert.x upgrade.
- **A custom `@VertxGen` API author** who wants Mutiny bindings for your own Vert.x-style interfaces.
- **A Vert.x extension author** producing a new client module that follows the Vert.x codegen conventions.
