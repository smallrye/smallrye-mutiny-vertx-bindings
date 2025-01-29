package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import io.smallrye.mutiny.vertx.MutinyGen;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class SomeTest {

    @Test
    void testSimpleDeclaration() {
        Env env = new Env();
        env.addJavaCode("me.escoffier.test", "MyInterface.java", """
                package me.escoffier.test;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface MyInterface {
                    void foo();
                }
                """);

        env.addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(outputs, "me.escoffier.test.MyInterface");
        JavaFile code = output.javaFile();
        assertThat(code.packageName()).isEqualTo("me.escoffier.test.mutiny");
        assertThat(code.typeSpec().name()).isEqualTo("MyInterface");
        assertThat(code.typeSpec().annotations()).contains(AnnotationSpec.builder(MutinyGen.class)
                .addMember("value", "me.escoffier.test.MyInterface.class").build());
        assertThat(code.typeSpec().modifiers()).contains(Modifier.PUBLIC);
        assertThat(code.typeSpec().kind()).isEqualTo(TypeSpec.Kind.CLASS);
        assertThat(code.typeSpec().javadoc().isEmpty()).isFalse();
        assertThat(code.toString())
                .contains("{@link me.escoffier.test.MyInterface")
                .contains("@see me.escoffier.test.MyInterface");
        assertThat(code.typeSpec().superinterfaces()).isEmpty();
        assertThat(code.typeSpec().superclass()).isEqualTo(TypeName.get(Object.class));

        env.compile();
    }

    @Test
    void testDeclarationWithTypeParameter() {
        Env env = new Env();
        env.addJavaCode("me.escoffier.test", "MyInterface.java", """
                package me.escoffier.test;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface MyInterface<T> {
                    T foo();
                }
                """);

        env.addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(outputs, "me.escoffier.test.MyInterface");
        JavaFile code = output.javaFile();
        assertThat(code.packageName()).isEqualTo("me.escoffier.test.mutiny");
        assertThat(code.typeSpec().name()).isEqualTo("MyInterface");
        assertThat(code.typeSpec().annotations()).contains(AnnotationSpec.builder(MutinyGen.class)
                .addMember("value", "me.escoffier.test.MyInterface.class").build());
        assertThat(code.typeSpec().modifiers()).contains(Modifier.PUBLIC);
        assertThat(code.typeSpec().kind()).isEqualTo(TypeSpec.Kind.CLASS);
        assertThat(code.typeSpec().javadoc().isEmpty()).isFalse();
        assertThat(code.toString())
                .contains("{@link me.escoffier.test.MyInterface")
                .contains("@see me.escoffier.test.MyInterface");
        assertThat(code.typeSpec().superinterfaces()).isEmpty();
        assertThat(code.typeSpec().superclass()).isEqualTo(TypeName.get(Object.class));

        env.compile();
    }

}
