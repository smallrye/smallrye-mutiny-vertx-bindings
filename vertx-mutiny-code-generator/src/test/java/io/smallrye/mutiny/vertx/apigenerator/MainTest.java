package io.smallrye.mutiny.vertx.apigenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

class MainTest {

    private static final String PACKAGE_INFO = """
            @ModuleGen(name = "acme", groupPackage = "org.acme")
            package org.acme;

            import io.vertx.codegen.annotations.ModuleGen;
            """;

    private static final String GREETER = """
            package org.acme;

            import io.vertx.codegen.annotations.VertxGen;
            import io.vertx.core.Future;

            @VertxGen
            public interface Greeter {
                String greet();

                Future<String> greetLater();
            }
            """;

    // Like io.vertx:vertx-bridge-common in Vert.x 5, which only contains a placeholder class
    private static final String DUMMY = """
            package io.vertx.eventbusbridge;

            public class Dummy {
            }
            """;

    @TempDir
    Path source;

    @TempDir
    Path output;

    @Test
    void validModuleIsGenerated() throws IOException {
        write("org/acme/package-info.java", PACKAGE_INFO);
        write("org/acme/Greeter.java", GREETER);

        assertThat(run("--module-name=acme")).isZero();
        assertThat(generatedFiles()).isEqualTo(1);
    }

    @Test
    void failureFailsMain() throws IOException {
        write("org/acme/package-info.java", PACKAGE_INFO);
        write("org/acme/Greeter.java", GREETER);

        assertThatThrownBy(() -> Main.main(new String[] {
                "--source=" + source, "--output=" + output, "--module-name=not-acme" }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The Mutiny bindings generator failed with exit code -1");
    }

    @Test
    void unknownModuleFails() throws IOException {
        write("org/acme/package-info.java", PACKAGE_INFO);
        write("org/acme/Greeter.java", GREETER);

        assertThat(run("--module-name=not-acme")).isEqualTo(-1);
        assertThatThrownBy(() -> new MutinyGenerator(source, "not-acme"))
                .hasRootCauseMessage("No module found with the module name not-acme");
    }

    @Test
    void emptySourcesFail() {
        assertThat(run("--module-name=acme")).isEqualTo(-1);
        assertThat(generatedFiles()).isZero();
    }

    @Test
    void sourcesWithoutVertxGenTypesFail() throws IOException {
        write("io/vertx/eventbusbridge/Dummy.java", DUMMY);

        assertThat(run("--module-name=vertx-bridge-common")).isEqualTo(-1);
    }

    @Test
    void vertxGenTypeWithoutModuleFails() throws IOException {
        write("org/acme/Greeter.java", GREETER);

        assertThat(run("--module-name=acme")).isEqualTo(-1);
    }

    @Test
    void moduleWithoutVertxGenTypesFails() throws IOException {
        write("org/acme/package-info.java", PACKAGE_INFO);

        assertThat(run("--module-name=acme")).isEqualTo(-1);
        assertThatThrownBy(() -> new MutinyGenerator(source, "acme").generate())
                .hasRootCauseMessage("No Vert.x Gen interface found in module acme");
    }

    @Test
    void unparsableSourceFails() throws IOException {
        write("org/acme/package-info.java", PACKAGE_INFO);
        write("org/acme/Greeter.java", GREETER);
        // The parser rejects this file, which would otherwise be silently ignored
        write("org/acme/Counter.java", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Counter {
                    default int next(int value) {
                        return value +;
                    }
                }
                """);

        assertThat(run("--module-name=acme")).isEqualTo(-1);
        assertThatThrownBy(() -> new MutinyGenerator(source, "acme"))
                .rootCause().hasMessageContaining("Counter.java");
    }

    @Test
    void sourcesWithoutVertxGenTypesAreAllowedWhenRequested() throws IOException {
        write("io/vertx/eventbusbridge/Dummy.java", DUMMY);

        assertThat(run("--module-name=vertx-bridge-common", "--allow-empty")).isZero();
        assertThat(generatedFiles()).isZero();
    }

    @Test
    void moduleWithoutVertxGenTypesIsAllowedWhenRequested() throws IOException {
        write("org/acme/package-info.java", PACKAGE_INFO);

        assertThat(run("--module-name=acme", "--allow-empty")).isZero();
        assertThat(generatedFiles()).isZero();
    }

    @Test
    void unparsableSourceFailsEvenWhenEmptyIsAllowed() throws IOException {
        write("org/acme/package-info.java", """
                @ModuleGen(name = "acme", groupPackage = "org.acme")
                package org.acme;

                import io.vertx.codegen.annotations.ModuleGen;

                class Broken {
                """);

        assertThat(run("--module-name=acme", "--allow-empty")).isEqualTo(-1);
    }

    @Test
    void validModuleIsGeneratedWhenEmptyIsAllowed() throws IOException {
        write("org/acme/package-info.java", PACKAGE_INFO);
        write("org/acme/Greeter.java", GREETER);

        assertThat(run("--module-name=acme", "--allow-empty")).isZero();
        assertThat(generatedFiles()).isEqualTo(1);
    }

    @Test
    void unknownModuleFailsEvenWhenEmptyIsAllowed() throws IOException {
        write("org/acme/package-info.java", PACKAGE_INFO);
        write("org/acme/Greeter.java", GREETER);

        // A wrong module name must not be mistaken for sources without Vert.x Gen types
        assertThat(run("--module-name=not-acme", "--allow-empty")).isEqualTo(-1);
    }

    @Test
    void emptySourcesFailEvenWhenEmptyIsAllowed() {
        // E.g., the unpacking of the sources produced nothing
        assertThat(run("--module-name=vertx-bridge-common", "--allow-empty")).isEqualTo(-1);
    }

    @Test
    void vertxGenTypeWithoutModuleFailsEvenWhenEmptyIsAllowed() throws IOException {
        write("org/acme/Greeter.java", GREETER);

        assertThat(run("--module-name=acme", "--allow-empty")).isEqualTo(-1);
    }

    @Test
    void recentJavaSyntaxIsAccepted(@TempDir Path additional) throws IOException {
        write("org/acme/package-info.java", PACKAGE_INFO);
        write("org/acme/Greeter.java", GREETER);
        // Guarded patterns are Java 21 syntax
        Path file = additional.resolve("org/other/Shapes.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                package org.other;

                public class Shapes {
                    static String describe(Object shape) {
                        return switch (shape) {
                            case String s when s.isEmpty() -> "empty";
                            default -> "other";
                        };
                    }
                }
                """);

        assertThat(run("--module-name=acme", "--additional-source=" + additional)).isZero();
        assertThat(generatedFiles()).isEqualTo(1);
    }

    @Test
    void writeFailureFails() throws IOException {
        write("org/acme/package-info.java", PACKAGE_INFO);
        write("org/acme/Greeter.java", GREETER);
        // The output cannot be a directory
        Files.delete(output);
        Files.createFile(output);

        assertThat(run("--module-name=acme")).isEqualTo(-1);
    }

    private int run(String... args) {
        String[] all = new String[args.length + 2];
        all[0] = "--source=" + source;
        all[1] = "--output=" + output;
        System.arraycopy(args, 0, all, 2, args.length);
        return new CommandLine(new Main()).execute(all);
    }

    private long generatedFiles() {
        try (Stream<Path> files = Files.walk(output)) {
            return files.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void write(String path, String content) throws IOException {
        Path file = source.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
