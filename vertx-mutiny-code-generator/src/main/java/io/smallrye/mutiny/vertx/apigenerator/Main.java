package io.smallrye.mutiny.vertx.apigenerator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

@CommandLine.Command(name = "vertx-mutiny-generator", mixinStandardHelpOptions = true, version = "Vert.x Mutiny Generator 1.0", description = "Entrypoint of the Vert.x Mutiny Generator")
public class Main implements Callable<Integer> {

    @CommandLine.Option(names = "--source", description = "The source directory", required = true)
    Path source;

    @CommandLine.Option(names = "--output", description = "The output directory, if not set the generation will not write the files on disk")
    Path output;

    @CommandLine.Option(names = "--additional-source", description = "Additional source directories, only to resolve symbols")
    List<Path> additionalSources = new ArrayList<>();

    @CommandLine.Option(names = "--module-name", description = "The name of the module to generate")
    String module;

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }

    @Override
    public Integer call() {
        MutinyGenerator generator = new MutinyGenerator(source, module, additionalSources.toArray(new Path[0]));
        // Collection happens during the construction.
        generator.analyze();
        List<MutinyGenerator.GeneratorOutput> list = generator.generate();
        if (output != null) {
            list.forEach(output -> {
                try {
                    output.javaFile().writeToPath(this.output);
                } catch (Exception e) {
                    logger.error("Unable to write the file for shim class: {}", output.shim().getFullyQualifiedName(), e);
                }
            });
        }

        return 0;
    }
}
