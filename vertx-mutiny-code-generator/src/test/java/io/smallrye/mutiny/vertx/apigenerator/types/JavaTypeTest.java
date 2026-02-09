package io.smallrye.mutiny.vertx.apigenerator.types;

import com.palantir.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JavaTypeTest {

    @Test
    void descriptions() {
        assertThat(new JavaType("foo.Bar").describe()).isEqualTo("foo.Bar");
        assertThat(new JavaType("Foo",
                List.of(
                        new JavaType("A"), new JavaType("B", List.of(new JavaType("C1"), new JavaType("C2"))),
                        new JavaType("D")))
                .describe())
                .isEqualTo("Foo<A,B<C1,C2>,D>");
    }

    @Test
    void invalidRepresentation() {
        assertThatThrownBy(() -> JavaType.of("List<Foo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Java shimType representation: List<Foo");
    }

    @Test
    void simpleClass() {
        assertThat(JavaType.of("java.lang.String"))
                .isEqualTo(new JavaType("java.lang.String"));
    }

    @Test
    void singleArityParametricTypes() {
        assertThat(JavaType.of("List<Integer>"))
                .isEqualTo(new JavaType("List", List.of(new JavaType("Integer"))));

        assertThat(JavaType.of("Set<List<Integer>>"))
                .isEqualTo(new JavaType("Set", List.of(
                        new JavaType("List", List.of(new JavaType("Integer"))))));
    }

    @Test
    void multiArityParametricTypes() {
        String type = "Foo<A,B<Bar,plop.Baz>,C<Yolo>>";
        JavaType parsed = JavaType.of(type);
        assertThat(parsed.describe()).isEqualTo(type);
        assertThat(parsed.fqn()).isEqualTo("Foo");
        assertThat(parsed.parameterTypes()).hasSize(3);
        assertThat(parsed.parameterTypes().get(1).parameterTypes()).hasSize(2);
    }

    @Test
    void toJavaPoetTypes() {
        JavaType parsed;

        parsed = JavaType.of("Integer");
        assertThat(parsed.toTypeName().toString()).isEqualTo("Integer");
        assertThat(parsed.hasParameterTypes()).isFalse();

        parsed = JavaType.of("Foo<A,B<Bar,plop.Baz>,C<Yolo>>");
        assertThat(parsed.hasParameterTypes()).isTrue();
        TypeName typeName = parsed.toTypeName();
        assertThat(typeName.toString()).isEqualTo("Foo<A, B<Bar, plop.Baz>, C<Yolo>>");
    }

    @Test
    void leniency() {
        JavaType parsed = JavaType.of(" Foo< A,  B< Bar , plop.Baz > , C< Yolo >  >");
        assertThat(parsed.hasParameterTypes()).isTrue();
        TypeName typeName = parsed.toTypeName();
        assertThat(typeName.toString()).isEqualTo("Foo<A, B<Bar, plop.Baz>, C<Yolo>>");
    }

    @Test
    void arrays() {
        JavaType parsed = JavaType.of("byte[]");
        TypeName typeName = parsed.toTypeName();
        assertThat(typeName.toString()).isEqualTo("byte[]");

        parsed = JavaType.of("java.lang.String[]");
        typeName = parsed.toTypeName();
        assertThat(typeName.toString()).isEqualTo("java.lang.String[]");
    }

    @Test
    void wildcardExtends() {
        JavaType parsed = JavaType.of("java.util.List<? extends java.lang.String>");
        TypeName typeName = parsed.toTypeName();
        assertThat(typeName.toString()).isEqualTo("java.util.List<? extends java.lang.String>");
    }

    @Test
    void wildcardSuper() {
        JavaType parsed = JavaType.of("java.util.List<? super java.lang.String>");
        TypeName typeName = parsed.toTypeName();
        assertThat(typeName.toString()).isEqualTo("java.util.List<? super java.lang.String>");
    }
}
