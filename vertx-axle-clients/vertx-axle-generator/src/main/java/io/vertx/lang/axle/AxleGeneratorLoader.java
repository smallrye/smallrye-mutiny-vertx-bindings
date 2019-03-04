package io.vertx.lang.axle;

import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;

import io.vertx.codegen.Generator;
import io.vertx.codegen.GeneratorLoader;

public class AxleGeneratorLoader implements GeneratorLoader {
    @Override
    public Stream<Generator<?>> loadGenerators(ProcessingEnvironment processingEnv) {
        return Stream.of(new AxleGenerator());
    }
}
