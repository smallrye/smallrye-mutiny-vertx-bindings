package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeVariableName;

import io.smallrye.mutiny.vertx.TypeArg;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class DelegateAndTypeArgConstructorShimTest {

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
    void testDelegateWithTypeParams() throws IOException {
        Env env = new Env();
        env.addJavaCode("me.escoffier.test", "MyInterface.java", """
                package me.escoffier.test;

                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface MyInterface<X,Y> {
                    X foo();
                    Y bar();
                }
                """);

        env.addModuleGen("me.escoffier.test", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(outputs, "me.escoffier.test.MyInterface");

        CompilationUnit parsed = StaticJavaParser.parse(output.javaFile().toString());
        assertThat(parsed).isNotNull();
        assertThat(output.javaFile().typeSpec().typeVariables()).hasSize(2).contains(TypeVariableName.get("X"),
                TypeVariableName.get("Y"));

        ParameterizedTypeName expectedDelegateType = ParameterizedTypeName
                .get(ClassName.get("me.escoffier.test", "MyInterface"), ClassName.bestGuess("X"), ClassName.bestGuess("Y"));

        // Check constructor - only delegate
        MethodSpec constructor = Env.findConstructor(output, "me.escoffier.test.MyInterface<X, Y>");
        assertThat(constructor.name()).isEqualTo("<init>");
        assertThat(constructor.parameters()).hasSize(1);
        assertThat(constructor.parameters().get(0).type()).isEqualTo(expectedDelegateType);
        assertThat(constructor.modifiers()).contains(Modifier.PUBLIC);
        assertThat(constructor.code().toString())
                .contains("this.delegate = delegate;")
                .contains("__typeArg_0");

        // Check constructor - only delegate + type arg
        constructor = Env.findConstructor(output, "me.escoffier.test.MyInterface<X, Y>",
                TypeArg.class.getName() + "<X>", TypeArg.class.getName() + "<Y>");
        assertThat(constructor.name()).isEqualTo("<init>");
        assertThat(constructor.parameters()).hasSize(3);
        assertThat(constructor.parameters().get(0).type()).isEqualTo(expectedDelegateType);
        assertThat(constructor.modifiers()).contains(Modifier.PUBLIC);
        assertThat(constructor.code().toString())
                .contains("this.delegate = delegate;")
                .contains("__typeArg_0")
                .contains("__typeArg_1");

        env.compile();
    }

}
