package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

public class DataObjectTest {

    @Test
    void testDataObjectTCK() {
        MutinyGenerator generator = new MutinyGenerator(Path.of("src/test/java/io/vertx/codegen/tck/dataobject"));
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(generator.generate(),
                "io.vertx.codegen.tck.dataobject.DataObjectTCK");
        String content = output.javaFile().toString();
        assertThat(content).contains("package io.vertx.codegen.tck.dataobject.mutiny;");

        assertThat(content)
                .contains("DataObjectWithValues getDataObjectWithValues()")
                .contains("getDelegate().getDataObjectWithValues()");
        ;

        assertThat(content)
                .contains("void setDataObjectWithValues(DataObjectWithValues dataObject)")
                .contains("getDelegate().setDataObjectWithValues(");

        assertThat(content)
                .contains("DataObjectWithLists getDataObjectWithLists()")
                .contains("getDelegate().getDataObjectWithLists()");

        assertThat(content)
                .contains("void setDataObjectWithLists(DataObjectWithLists dataObject)")
                .contains("getDelegate().setDataObjectWithLists");

        assertThat(content)
                .contains("DataObjectWithMaps getDataObjectWithMaps()")
                .contains("getDelegate().getDataObjectWithMaps()");

        assertThat(content)
                .contains("void setDataObjectWithMaps(DataObjectWithMaps dataObject)")
                .contains("getDelegate().setDataObjectWithMaps");
    }

    @Test
    void compileDataObjectTCK() {
        Env env = new Env();
        env.addPackage("src/test/java", "io.vertx.codegen.tck.dataobject");
        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();
    }

}
