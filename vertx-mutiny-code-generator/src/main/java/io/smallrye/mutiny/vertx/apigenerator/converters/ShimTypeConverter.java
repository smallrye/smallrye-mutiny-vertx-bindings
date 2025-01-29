package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;

/**
 * An interface implemented by classes taking care of converting types from the original Vert.x types to the Mutiny types.
 */
public interface ShimTypeConverter {

    /**
     * Whether the converter can convert the given shimType.
     *
     * @param type the shimType to check
     * @return {@code true} if the converter can convert the shimType, {@code false} otherwise
     */
    boolean accept(ResolvedType type);

    /**
     * Converts the given shimType.
     *
     * @param type the shimType to convert
     * @return the converted shimType, may not be resolvable
     */
    Type convert(ResolvedType type);

    default ShimTypeConverter configure(MutinyGenerator generator) {
        // Do nothing by default
        return this;
    }
}
