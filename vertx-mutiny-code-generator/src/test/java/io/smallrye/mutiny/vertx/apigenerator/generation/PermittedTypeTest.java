package io.smallrye.mutiny.vertx.apigenerator.generation;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PermittedTypeTest {

    @Test
    void runAnyTypeTCK() {
        Env env = new Env();
        env.addPackage("src/test/java", "io.vertx.codegen.tck.anytype")
                .addModuleGen("io.vertx.codegen.tck.anytype", "anytype");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();

        JavaFile content = env.getOutputFor("io.vertx.codegen.tck.anytype.AnyJavaTypeTCK").javaFile();
        assertThat(content.typeSpec().methodSpecs()).anySatisfy(this::methodWithJavaTypeParam);
        assertThat(content.typeSpec().methodSpecs()).anySatisfy(this::methodWithJavaTypeReturn);
    }

    void methodWithJavaTypeParam(MethodSpec method) {
        assertThat(method.name()).isEqualTo("methodWithJavaTypeParam");
        assertThat(method.parameters()).hasSize(1);
        assertThat(method.parameters().get(0).type().toString()).isEqualTo("java.net.Socket");
        assertThat(method.returnType()).isEqualTo(TypeName.VOID);
    }

    void methodWithJavaTypeReturn(MethodSpec method) {
        assertThat(method.name()).isEqualTo("methodWithJavaTypeReturn");
        assertThat(method.parameters()).hasSize(0);
        assertThat(method.returnType().toString()).isEqualTo("java.net.Socket");
    }

}
