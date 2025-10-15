package io.smallrye.mutiny.vertx.apigenerator.analysis;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenInterface;
import io.smallrye.mutiny.vertx.apigenerator.shims.CompanionShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.ConstantShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.DelegateAndTypeArgsConstructorShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.DelegateAsObjectAndTypeArgsConstructorShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.DelegateConstructorShim;
import io.smallrye.mutiny.vertx.apigenerator.shims.DelegateShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.EqualsHashCodeAndToStringShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.FunctionShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.HandlerShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.HierarchyShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.IterableShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.IteratorShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.NewInstanceMethodShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.NoArgConstructorShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.PlainMethodShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.ReadStreamModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.TypeArgConstantShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.UniMethodShimModule;
import io.smallrye.mutiny.vertx.apigenerator.shims.WriteStreamModule;

/**
 * Analyze the result of the collection and build the necessary shims.
 */
public class VertxGenAnalysis {

    private static final Logger logger = LoggerFactory.getLogger(VertxGenAnalysis.class);
    private final MutinyGenerator generator;

    public VertxGenAnalysis(MutinyGenerator generator) {
        this.generator = generator;
    }

    public AnalysisResult analyze() {
        // Normally, shim module are intended to be stateless, and so this list could be static.
        // But, for the sake of the safety, we create a new instance of each module for each analysis.
        List<ShimModule> modules = new ArrayList<>();
        modules.add(new HierarchyShimModule());
        modules.add(new HandlerShimModule());
        modules.add(new FunctionShimModule());
        modules.add(new IteratorShimModule());
        modules.add(new IterableShimModule());
        modules.add(new DelegateShimModule());
        modules.add(new ConstantShimModule());
        modules.add(new DelegateConstructorShim());
        modules.add(new UniMethodShimModule());
        modules.add(new TypeArgConstantShimModule());
        modules.add(new PlainMethodShimModule());
        modules.add(new NewInstanceMethodShimModule());
        modules.add(new NoArgConstructorShimModule());
        modules.add(new DelegateAndTypeArgsConstructorShimModule());
        modules.add(new DelegateAsObjectAndTypeArgsConstructorShimModule());
        modules.add(new EqualsHashCodeAndToStringShimModule());
        modules.add(new CompanionShimModule());
        modules.add(new ReadStreamModule());
        modules.add(new WriteStreamModule());

        List<ShimClass> shims = new ArrayList<>();

        for (VertxGenInterface anInterface : generator.getCollectionResult().interfaces()) {
            logger.debug("Interface {} has been collected", anInterface.getFullyQualifiedName());
        }
        for (VertxGenInterface itf : generator.getCollectionResult().interfaces()) {
            logger.debug("Analyzing {}", itf.getFullyQualifiedName());
            ShimClass shim = new ShimClass(itf);
            for (ShimModule module : modules) {
                if (module.accept(shim)) {
                    module.analyze(shim);
                }
            }

            // If we have a companion class added to the shim, we need to analyze the companion too with a subset of modules
            if (shim.getCompanion() != null) {
                ShimCompanionClass companion = shim.getCompanion();
                List<ShimModule> companionModules = new ArrayList<>();
                companionModules.add(new DelegateShimModule());
                companionModules.add(new DelegateConstructorShim());
                companionModules.add(new DelegateAndTypeArgsConstructorShimModule());
                companionModules.add(new NoArgConstructorShimModule());
                companionModules.add(new UniMethodShimModule());
                companionModules.add(new PlainMethodShimModule());
                companionModules.add(new ReadStreamModule());
                for (ShimModule module : companionModules) {
                    // We skip the accept method as we know the companion is not concrete,
                    // the read stream module will need to check if the source is a read stream in the analyze method.
                    module.analyze(companion);
                }
            }

            shims.add(shim);
        }

        return new AnalysisResult(shims);
    }

}
