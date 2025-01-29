package io.smallrye.mutiny.vertx.apigenerator.converters;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.types.ResolvedTypeDescriber;

public class Converters {

    private final List<ShimTypeConverter> converters = new ArrayList<>();

    public Converters(MutinyGenerator generator) {
        converters.add(new VoidConverter().configure(generator));
        converters.add(new StringConverter().configure(generator));
        converters.add(new JavaLangObjectConverter().configure(generator));
        converters.add(new ClassConverter().configure(generator));
        converters.add(new EnumConverter().configure(generator));
        converters.add(new DataObjectConverter().configure(generator));
        converters.add(new PrimitiveTypeConverter().configure(generator));
        converters.add(new BoxedPrimitiveTypeConverter().configure(generator));
        converters.add(new VertxJsonConverter().configure(generator));
        converters.add(new ThrowableConverter().configure(generator));
        converters.add(new ListAndSetConverter().configure(generator));
        converters.add(new MapConverter().configure(generator));
        converters.add(new SupplierConverter().configure(generator));
        converters.add(new VertxGenInterfaceConverter().configure(generator));
        converters.add(new VertxFutureTypeConverter().configure(generator));
        converters.add(new VertxHandlerConverter().configure(generator));
        converters.add(new VertxAsyncResultConverter().configure(generator));
        converters.add(new FunctionConverter().configure(generator));
    }

    public Type convert(ResolvedType type) {
        if (type.isTypeVariable()) {
            return StaticJavaParser.parseTypeParameter(type.asTypeVariable().describe());
        }
        for (ShimTypeConverter converter : converters) {
            if (converter.accept(type)) {
                return converter.convert(type);
            }
        }
        String described = ResolvedTypeDescriber.describeResolvedType(type);
        return StaticJavaParser.parseType(described);
    }

}
