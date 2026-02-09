package io.smallrye.mutiny.vertx.apigenerator;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.utils.SourceRoot;
import com.palantir.javapoet.JavaFile;
import io.smallrye.mutiny.vertx.apigenerator.analysis.AnalysisResult;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.analysis.VertxGenAnalysis;
import io.smallrye.mutiny.vertx.apigenerator.collection.CollectionResult;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenCollection;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenInterface;
import io.smallrye.mutiny.vertx.apigenerator.converters.Converters;
import io.smallrye.mutiny.vertx.apigenerator.generator.ShimGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MutinyGenerator {

    private final Converters converters;

    private static final Logger logger = LoggerFactory.getLogger(MutinyGenerator.class);

    private final CollectionResult collection;
    private final VertxGenAnalysis analyzer;
    private AnalysisResult analysisResult;

    public MutinyGenerator(Path source, String module, Path... additionalSources) {
        this(new SourceRoot(source), module, additionalSources);
    }

    public MutinyGenerator(SourceRoot root, String module, Path... additionalSources) {
        this.converters = new Converters(this);
        try {
            this.collection = new VertxGenCollection(this, root, Arrays.asList(additionalSources)).collect(module);
        } catch (Exception e) {
            logger.error("Unable to run collection", e);
            throw new IllegalStateException("Unable to run collection", e);
        }
        this.analyzer = new VertxGenAnalysis(this);
    }

    public MutinyGenerator(Path source) {
        this(source, null);
    }

    public Type convertType(ResolvedType type) {
        return converters.convert(type);
    }

    public CollectionResult getCollectionResult() {
        return collection;
    }

    public Converters getConverters() {
        return converters;
    }

    public AnalysisResult analyze() {
        if (analysisResult == null) {
            analysisResult = analyzer.analyze();
        }
        return analysisResult;
    }

    public record GeneratorOutput(VertxGenInterface genInterface, ShimClass shim, JavaFile javaFile) {
    }

    public List<GeneratorOutput> generate() {
        List<ShimClass> shims = analyze().shims();

        List<GeneratorOutput> output = new ArrayList<>();
        for (ShimClass shim : shims) {
            ShimGenerator generator = new ShimGenerator(shim);
            var f = generator.generate();
            GeneratorOutput go = new GeneratorOutput(shim.getSource(), shim, f);
            output.add(go);
        }
        return output;
    }
}
