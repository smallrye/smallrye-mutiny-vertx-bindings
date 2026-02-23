package io.smallrye.mutiny.vertx.apigenerator.converters;

import com.github.javaparser.resolution.types.ResolvedType;

public class VertxJsonConverter extends BaseShimTypeConverter {

    public static final String JSON_OBJECT = "io.vertx.core.json.JsonObject";
    public static final String JSON_ARRAY = "io.vertx.core.json.JsonArray";

    @Override
    public boolean accept(ResolvedType type) {
        return type.isReferenceType() &&
                (type.asReferenceType().getQualifiedName().equals(JSON_OBJECT)
                        || type.asReferenceType().getQualifiedName().equals(JSON_ARRAY));
    }
}
