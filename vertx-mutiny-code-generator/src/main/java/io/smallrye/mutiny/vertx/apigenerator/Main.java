package io.smallrye.mutiny.vertx.apigenerator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.vertx.apigenerator.collection.NothingToGenerateException;
import picocli.CommandLine;

@CommandLine.Command(name = "vertx-mutiny-generator", mixinStandardHelpOptions = true, description = "Mutiny Vert.x 5 Bindings Generator")
public class Main implements Callable<Integer> {

    @CommandLine.Option(names = "--source", description = "The source directory", required = true)
    Path source;

    @CommandLine.Option(names = "--output", description = "The output directory, if not set the generation will not write the files on disk")
    Path output;

    @CommandLine.Option(names = "--additional-source", description = "Additional source directories, only to resolve symbols")
    List<Path> additionalSources = new ArrayList<>();

    @CommandLine.Option(names = "--module-name", description = "The name of the module to generate")
    String module;

    @CommandLine.Option(names = "--allow-empty", description = "Succeed with a warning when there is nothing to generate")
    boolean allowEmpty;

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        if (exitCode != 0) {
            // Not System.exit: it would also terminate the Maven JVM when running with exec:java
            throw new IllegalStateException("The Mutiny bindings generator failed with exit code " + exitCode);
        }
    }

    @Override
    public Integer call() {
        try {
            MutinyGenerator generator = new MutinyGenerator(source, module, additionalSources.toArray(new Path[0]));
            // Collection happens during the construction.
            generator.analyze();
            List<MutinyGenerator.GeneratorOutput> list = generator.generate();
            if (output != null) {
                for (MutinyGenerator.GeneratorOutput generatorOutput : list) {
                    try {
                        generatorOutput.javaFile().writeToPath(this.output);
                    } catch (Exception e) {
                        logger.error("Unable to write the file for shim class: {}",
                                generatorOutput.shim().getFullyQualifiedName(), e);
                        return -1;
                    }
                }
            }
            return 0;
        } catch (Throwable err) {
            if (allowEmpty) {
                for (Throwable cause = err; cause != null; cause = cause.getCause()) {
                    if (cause instanceof NothingToGenerateException) {
                        logger.warn("Nothing to generate: {}", cause.getMessage());
                        return 0;
                    }
                }
            }
            logger.error("The generator encountered a fatal error", err);
            return -1;
        }
    }
}
