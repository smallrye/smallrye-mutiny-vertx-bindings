package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

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

public class NewInstanceMethodShimTest {

    @Test
    void testDelegate() {
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

        CompilationUnit parsed = StaticJavaParser.parse(output.javaFile().toString());
        assertThat(parsed).isNotNull();
        MethodSpec method = Env.findMethod(output, "newInstance", "me.escoffier.test.MyInterface");
        assertThat(method.name()).isEqualTo("newInstance");
        assertThat(method.parameters()).hasSize(1);
        assertThat(method.parameters().get(0).type())
                .isEqualTo(ClassName.get("me.escoffier.test", "MyInterface"));
        assertThat(method.modifiers()).contains(Modifier.PUBLIC);
        assertThat(method.code().toString()).contains("me.escoffier.test.mutiny.MyInterface(delegate)")
                .doesNotContain("typeArg"); // No generics

        env.compile();
    }

    @Test
    void testDelegateWithTypeParams() {
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

        MethodSpec method = Env.findMethod(output, "newInstance", "me.escoffier.test.MyInterface<X, Y>",
                TypeArg.class.getName() + "<X>", TypeArg.class.getName() + "<Y>");
        assertThat(method.name()).isEqualTo("newInstance");
        assertThat(method.parameters()).hasSize(3);
        assertThat(method.parameters().get(0).type()).isEqualTo(expectedDelegateType);
        assertThat(method.modifiers()).contains(Modifier.PUBLIC);
        assertThat(method.code().toString())
                .contains("me.escoffier.test.mutiny.MyInterface<X, Y>(delegate, typeArg_0, typeArg_1)");
        env.compile();
    }

}
