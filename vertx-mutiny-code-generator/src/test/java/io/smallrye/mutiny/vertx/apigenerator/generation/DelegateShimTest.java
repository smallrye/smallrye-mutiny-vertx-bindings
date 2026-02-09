package io.smallrye.mutiny.vertx.apigenerator.generation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.palantir.javapoet.*;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DelegateShimTest {

    @Test
    void testDelegate() {
        Env creator = new Env();
        creator.addJavaCode("me.escoffier.test", "MyInterface.java", """
                package me.escoffier.test;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface MyInterface {
                    void foo();
                }
                """);

        creator.addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(creator.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(outputs, "me.escoffier.test.MyInterface");

        CompilationUnit parsed = StaticJavaParser.parse(output.javaFile().toString());
        assertThat(parsed).isNotNull();

        // Check field
        FieldSpec field = Env.findField(output, "delegate");
        assertThat(field.name()).isEqualTo("delegate");
        assertThat(field.type()).isEqualTo(ClassName.get("me.escoffier.test", "MyInterface"));
        assertThat(field.modifiers()).contains(Modifier.PRIVATE);
        assertThat(field.modifiers()).contains(Modifier.FINAL);
        assertThat(field.modifiers()).doesNotContain(Modifier.STATIC);

        // Check getter
        MethodSpec getter = Env.findMethod(output, "getDelegate");
        assertThat(getter.name()).isEqualTo("getDelegate");
        assertThat(getter.returnType()).isEqualTo(ClassName.get("me.escoffier.test", "MyInterface"));
        assertThat(getter.modifiers()).contains(Modifier.PUBLIC);
        assertThat(getter.modifiers()).doesNotContain(Modifier.STATIC);
        assertThat(getter.code().toString()).contains("return delegate;");

        // Check constructor
        MethodSpec constructor = Env.findConstructor(output, "me.escoffier.test.MyInterface");
        assertThat(constructor.name()).isEqualTo("<init>");
        assertThat(constructor.parameters()).hasSize(1);
        assertThat(constructor.parameters().get(0).type())
                .isEqualTo(ClassName.get("me.escoffier.test", "MyInterface"));
        assertThat(constructor.modifiers()).contains(Modifier.PUBLIC);
        assertThat(constructor.code().toString()).contains("this.delegate = delegate;")
                .doesNotContain("typeArg"); // No generics

    }

    @Test
    void testDelegateWithATypeParam() {
        Env env = new Env();
        env.addJavaCode("me.escoffier.test", "MyInterface.java", """
                package me.escoffier.test;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface MyInterface<X> {
                    X foo();
                }
                """);

        env.addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(outputs, "me.escoffier.test.MyInterface");

        CompilationUnit parsed = StaticJavaParser.parse(output.javaFile().toString());
        assertThat(parsed).isNotNull();
        assertThat(output.javaFile().typeSpec().typeVariables()).hasSize(1).contains(TypeVariableName.get("X"));

        ParameterizedTypeName expectedDelegateType = ParameterizedTypeName
                .get(ClassName.get("me.escoffier.test", "MyInterface"), ClassName.bestGuess("X"));
        ParameterizedTypeName expectedTypeArg0 = ParameterizedTypeName.get(ClassName.get("io.smallrye.mutiny.vertx", "TypeArg"),
                ClassName.bestGuess("X"));

        // Check field
        FieldSpec field = Env.findField(output, "delegate");
        assertThat(field.name()).isEqualTo("delegate");
        assertThat(field.type()).isEqualTo(expectedDelegateType);
        assertThat(field.modifiers()).contains(Modifier.PRIVATE);
        assertThat(field.modifiers()).contains(Modifier.FINAL);
        assertThat(field.modifiers()).doesNotContain(Modifier.STATIC);

        field = Env.findField(output, "__typeArg_0");
        assertThat(field.name()).isEqualTo("__typeArg_0");
        assertThat(field.type()).isEqualTo(expectedTypeArg0);
        assertThat(field.modifiers()).contains(Modifier.FINAL);
        assertThat(field.modifiers()).doesNotContain(Modifier.STATIC);

        // Check getter
        MethodSpec getter = Env.findMethod(output, "getDelegate");
        assertThat(getter.name()).isEqualTo("getDelegate");
        assertThat(getter.returnType()).isEqualTo(expectedDelegateType);
        assertThat(getter.modifiers()).contains(Modifier.PUBLIC);
        assertThat(getter.modifiers()).doesNotContain(Modifier.STATIC);
        assertThat(getter.code().toString()).contains("return delegate;");

        // Check constructor
        MethodSpec constructor = Env.findConstructor(output, "me.escoffier.test.MyInterface<X>");
        assertThat(constructor.name()).isEqualTo("<init>");
        assertThat(constructor.parameters()).hasSize(1);
        assertThat(constructor.parameters().get(0).type()).isEqualTo(expectedDelegateType);
        assertThat(constructor.modifiers()).contains(Modifier.PUBLIC);
        assertThat(constructor.code().toString())
                .contains("this.delegate = delegate;")
                .contains("__typeArg_0");

        env.compile();
    }

}
