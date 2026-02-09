package io.smallrye.mutiny.vertx.apigenerator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.apigenerator.utils.TypeUtils;

class TypeUtilsTest {

    @Test
    void buildParameterizedType() {
        Type t1 = StaticJavaParser.parseClassOrInterfaceType("java.lang.String");
        assertThat(TypeUtils.buildParameterizedType(Uni.class, t1).asString())
                .isEqualTo("io.smallrye.mutiny.Uni<java.lang.String>");

        t1 = StaticJavaParser.parseType("java.lang.Integer");
        assertThat(TypeUtils.buildParameterizedType(Uni.class, t1).asString())
                .isEqualTo("io.smallrye.mutiny.Uni<java.lang.Integer>");

        t1 = StaticJavaParser.parseClassOrInterfaceType("java.util.List<java.lang.String>");
        assertThat(TypeUtils.buildParameterizedType(Uni.class, t1).asString())
                .isEqualTo("io.smallrye.mutiny.Uni<java.util.List<java.lang.String>>");

        t1 = StaticJavaParser.parseTypeParameter("T");
        Type type = TypeUtils.buildParameterizedType(Uni.class, t1);
        assertThat(type.asString()).isEqualTo("io.smallrye.mutiny.Uni<T>");
        assertThat(type.asClassOrInterfaceType().getTypeArguments()).isPresent().hasValueSatisfying(l -> {
            assertThat(l).hasSize(1);
            assertThat(l.getFirst().get().isTypeParameter()).isTrue();
            assertThat(l.getFirst().get().asTypeParameter().asString()).isEqualTo("T");
        });
    }

}
