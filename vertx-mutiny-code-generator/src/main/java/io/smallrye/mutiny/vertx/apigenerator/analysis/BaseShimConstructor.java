package io.smallrye.mutiny.vertx.apigenerator.analysis;

import com.github.javaparser.ast.type.Type;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseShimConstructor implements ShimConstructor {

    private final List<ShimMethodParameter> parameters = new ArrayList<>();
    private final List<Type> throwsTypes = new ArrayList<>();
    private final ShimModule module;

    public BaseShimConstructor(ShimModule module) {
        this.module = module;
    }

    public BaseShimConstructor(ShimModule module, List<ShimMethodParameter> parameters) {
        this(module);
        this.parameters.addAll(parameters);
    }

    public BaseShimConstructor(ShimModule module, List<ShimMethodParameter> parameters, List<Type> throwsTypes) {
        this(module);
        this.parameters.addAll(parameters);
        this.throwsTypes.addAll(throwsTypes);
    }

    @Override
    public ShimModule declaredBy() {
        return module;
    }

    @Override
    public List<ShimMethodParameter> getParameters() {
        return parameters;
    }

    @Override
    public List<Type> getThrows() {
        return throwsTypes;
    }
}
