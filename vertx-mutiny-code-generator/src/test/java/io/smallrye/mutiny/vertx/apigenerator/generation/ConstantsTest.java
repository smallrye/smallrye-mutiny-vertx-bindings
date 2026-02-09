package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConstantsTest {

    @Test
    void testConstantsTCK() {
        MutinyGenerator generator = new MutinyGenerator(Path.of("src/test/java/io/vertx/codegen/tck/constants"));
        MutinyGenerator.GeneratorOutput output = Env.getOutputFor(generator.generate(),
                "io.vertx.codegen.tck.constants.ConstantTCK");

        String content = output.javaFile().toString();

        assertThat(content).contains("Some doc.");
        assertThat(content).contains("public static final byte BYTE = io.vertx.codegen.tck.constants.ConstantTCK.BYTE;");
        assertThat(content)
                .contains("public static final Byte BOXED_BYTE = io.vertx.codegen.tck.constants.ConstantTCK.BOXED_BYTE;");

        assertThat(content).contains("public static final short SHORT = io.vertx.codegen.tck.constants.ConstantTCK.SHORT;");
        assertThat(content)
                .contains("public static final Short BOXED_SHORT = io.vertx.codegen.tck.constants.ConstantTCK.BOXED_SHORT;");
        assertThat(content).contains("public static final int INT = io.vertx.codegen.tck.constants.ConstantTCK.INT;");
        assertThat(content)
                .contains("public static final Integer BOXED_INT = io.vertx.codegen.tck.constants.ConstantTCK.BOXED_INT;");
        assertThat(content).contains("public static final long LONG = io.vertx.codegen.tck.constants.ConstantTCK.LONG;");
        assertThat(content)
                .contains("public static final Long BOXED_LONG = io.vertx.codegen.tck.constants.ConstantTCK.BOXED_LONG;");
        assertThat(content).contains("public static final float FLOAT = io.vertx.codegen.tck.constants.ConstantTCK.FLOAT;");
        assertThat(content)
                .contains("public static final Float BOXED_FLOAT = io.vertx.codegen.tck.constants.ConstantTCK.BOXED_FLOAT;");
        assertThat(content).contains("public static final double DOUBLE = io.vertx.codegen.tck.constants.ConstantTCK.DOUBLE;");
        assertThat(content)
                .contains("public static final Double BOXED_DOUBLE = io.vertx.codegen.tck.constants.ConstantTCK.BOXED_DOUBLE;");
        assertThat(content)
                .contains("public static final boolean BOOLEAN = io.vertx.codegen.tck.constants.ConstantTCK.BOOLEAN;");
        assertThat(content).contains(
                "public static final Boolean BOXED_BOOLEAN = io.vertx.codegen.tck.constants.ConstantTCK.BOXED_BOOLEAN;");
        assertThat(content).contains("public static final char CHAR = io.vertx.codegen.tck.constants.ConstantTCK.CHAR;");
        assertThat(content)
                .contains("public static final Character BOXED_CHAR = io.vertx.codegen.tck.constants.ConstantTCK.BOXED_CHAR;");
        assertThat(content).contains("public static final String STRING = io.vertx.codegen.tck.constants.ConstantTCK.STRING;");

        assertThat(content).contains(
                "public static final RefedInterface1 VERTX_GEN = io.vertx.codegen.tck.constants.mutiny.RefedInterface1.newInstance((io.vertx.codegen.tck.constants.RefedInterface1) io.vertx.codegen.tck.constants.ConstantTCK.VERTX_GEN);");

        assertThat(content).contains(
                "public static final TestDataObject DATA_OBJECT = io.vertx.codegen.tck.constants.ConstantTCK.DATA_OBJECT;");
        assertThat(content).contains(
                "public static final JsonObject JSON_OBJECT = io.vertx.codegen.tck.constants.ConstantTCK.JSON_OBJECT;");
        assertThat(content)
                .contains("public static final JsonArray JSON_ARRAY = io.vertx.codegen.tck.constants.ConstantTCK.JSON_ARRAY;");
        assertThat(content).contains("public static final TestEnum ENUM = io.vertx.codegen.tck.constants.ConstantTCK.ENUM;");
        assertThat(content)
                .contains("public static final Throwable THROWABLE = io.vertx.codegen.tck.constants.ConstantTCK.THROWABLE;");
        assertThat(content).contains("public static final Object OBJECT = io.vertx.codegen.tck.constants.ConstantTCK.OBJECT;");

        assertThat(content).contains(
                "public static final RefedInterface1 NULLABLE_NON_NULL = io.vertx.codegen.tck.constants.mutiny.RefedInterface1.newInstance((io.vertx.codegen.tck.constants.RefedInterface1) io.vertx.codegen.tck.constants.ConstantTCK.NULLABLE_NON_NULL);");
        assertThat(content).contains(
                "public static final RefedInterface1 NULLABLE_NULL = io.vertx.codegen.tck.constants.mutiny.RefedInterface1.newInstance((io.vertx.codegen.tck.constants.RefedInterface1) io.vertx.codegen.tck.constants.ConstantTCK.NULLABLE_NULL);");

        assertThat(content)
                .contains("public static final List<Byte> BYTE_LIST = io.vertx.codegen.tck.constants.ConstantTCK.BYTE_LIST;");
        assertThat(content).contains(
                "public static final List<Short> SHORT_LIST = io.vertx.codegen.tck.constants.ConstantTCK.SHORT_LIST;");
        assertThat(content)
                .contains("public static final List<Integer> INT_LIST = io.vertx.codegen.tck.constants.ConstantTCK.INT_LIST;");
        assertThat(content)
                .contains("public static final List<Long> LONG_LIST = io.vertx.codegen.tck.constants.ConstantTCK.LONG_LIST;");
        assertThat(content).contains(
                "public static final List<Float> FLOAT_LIST = io.vertx.codegen.tck.constants.ConstantTCK.FLOAT_LIST;");
        assertThat(content).contains(
                "public static final List<Double> DOUBLE_LIST = io.vertx.codegen.tck.constants.ConstantTCK.DOUBLE_LIST;");
        assertThat(content).contains(
                "public static final List<Boolean> BOOLEAN_LIST = io.vertx.codegen.tck.constants.ConstantTCK.BOOLEAN_LIST;");
        assertThat(content).contains(
                "public static final List<Character> CHAR_LIST = io.vertx.codegen.tck.constants.ConstantTCK.CHAR_LIST;");
        assertThat(content).contains(
                "public static final List<String> STRING_LIST = io.vertx.codegen.tck.constants.ConstantTCK.STRING_LIST;");

        assertThat(content).contains(
                "public static final List<RefedInterface1> VERTX_GEN_LIST = io.vertx.codegen.tck.constants.ConstantTCK.VERTX_GEN_LIST.stream().map(item -> io.vertx.codegen.tck.constants.mutiny.RefedInterface1.newInstance((io.vertx.codegen.tck.constants.RefedInterface1)item)).collect(java.util.stream.Collectors.toList());");

        assertThat(content).contains(
                "public static final List<JsonObject> JSON_OBJECT_LIST = io.vertx.codegen.tck.constants.ConstantTCK.JSON_OBJECT_LIST;");
        assertThat(content).contains(
                "public static final List<JsonArray> JSON_ARRAY_LIST = io.vertx.codegen.tck.constants.ConstantTCK.JSON_ARRAY_LIST;");
        assertThat(content).contains(
                "public static final List<TestDataObject> DATA_OBJECT_LIST = io.vertx.codegen.tck.constants.ConstantTCK.DATA_OBJECT_LIST;");
        assertThat(content).contains(
                "public static final List<TestEnum> ENUM_LIST = io.vertx.codegen.tck.constants.ConstantTCK.ENUM_LIST;");

        assertThat(content)
                .contains("public static final Set<Byte> BYTE_SET = io.vertx.codegen.tck.constants.ConstantTCK.BYTE_SET;");
        assertThat(content)
                .contains("public static final Set<Short> SHORT_SET = io.vertx.codegen.tck.constants.ConstantTCK.SHORT_SET;");
        assertThat(content)
                .contains("public static final Set<Integer> INT_SET = io.vertx.codegen.tck.constants.ConstantTCK.INT_SET;");
        assertThat(content)
                .contains("public static final Set<Long> LONG_SET = io.vertx.codegen.tck.constants.ConstantTCK.LONG_SET;");
        assertThat(content)
                .contains("public static final Set<Float> FLOAT_SET = io.vertx.codegen.tck.constants.ConstantTCK.FLOAT_SET;");
        assertThat(content).contains(
                "public static final Set<Double> DOUBLE_SET = io.vertx.codegen.tck.constants.ConstantTCK.DOUBLE_SET;");
        assertThat(content).contains(
                "public static final Set<Boolean> BOOLEAN_SET = io.vertx.codegen.tck.constants.ConstantTCK.BOOLEAN_SET;");
        assertThat(content)
                .contains("public static final Set<Character> CHAR_SET = io.vertx.codegen.tck.constants.ConstantTCK.CHAR_SET;");
        assertThat(content).contains(
                "public static final Set<String> STRING_SET = io.vertx.codegen.tck.constants.ConstantTCK.STRING_SET;");

        assertThat(content).contains(
                "public static final Set<RefedInterface1> VERTX_GEN_SET = io.vertx.codegen.tck.constants.ConstantTCK.VERTX_GEN_SET.stream().map(item -> io.vertx.codegen.tck.constants.mutiny.RefedInterface1.newInstance((io.vertx.codegen.tck.constants.RefedInterface1)item)).collect(java.util.stream.Collectors.toSet());");
        assertThat(content).contains(
                "public static final Set<JsonObject> JSON_OBJECT_SET = io.vertx.codegen.tck.constants.ConstantTCK.JSON_OBJECT_SET;");
        assertThat(content).contains(
                "public static final Set<JsonArray> JSON_ARRAY_SET = io.vertx.codegen.tck.constants.ConstantTCK.JSON_ARRAY_SET;");
        assertThat(content).contains(
                "public static final Set<TestDataObject> DATA_OBJECT_SET = io.vertx.codegen.tck.constants.ConstantTCK.DATA_OBJECT_SET;");
        assertThat(content)
                .contains("public static final Set<TestEnum> ENUM_SET = io.vertx.codegen.tck.constants.ConstantTCK.ENUM_SET;");
        assertThat(content).contains(
                "public static final Map<String, Byte> BYTE_MAP = io.vertx.codegen.tck.constants.ConstantTCK.BYTE_MAP;");
        assertThat(content).contains(
                "public static final Map<String, Short> SHORT_MAP = io.vertx.codegen.tck.constants.ConstantTCK.SHORT_MAP;");
        assertThat(content).contains(
                "public static final Map<String, Integer> INT_MAP = io.vertx.codegen.tck.constants.ConstantTCK.INT_MAP;");
        assertThat(content).contains(
                "public static final Map<String, Long> LONG_MAP = io.vertx.codegen.tck.constants.ConstantTCK.LONG_MAP;");
        assertThat(content).contains(
                "public static final Map<String, Float> FLOAT_MAP = io.vertx.codegen.tck.constants.ConstantTCK.FLOAT_MAP;");
        assertThat(content).contains(
                "public static final Map<String, Double> DOUBLE_MAP = io.vertx.codegen.tck.constants.ConstantTCK.DOUBLE_MAP;");
        assertThat(content).contains(
                "public static final Map<String, Boolean> BOOLEAN_MAP = io.vertx.codegen.tck.constants.ConstantTCK.BOOLEAN_MAP;");
        assertThat(content).contains(
                "public static final Map<String, Character> CHAR_MAP = io.vertx.codegen.tck.constants.ConstantTCK.CHAR_MAP;");
        assertThat(content).contains(
                "public static final Map<String, String> STRING_MAP = io.vertx.codegen.tck.constants.ConstantTCK.STRING_MAP;");
        assertThat(content).contains(
                "public static final Map<String, JsonObject> JSON_OBJECT_MAP = io.vertx.codegen.tck.constants.ConstantTCK.JSON_OBJECT_MAP;");
        assertThat(content).contains(
                "public static final Map<String, JsonArray> JSON_ARRAY_MAP = io.vertx.codegen.tck.constants.ConstantTCK.JSON_ARRAY_MAP;");

    }

    @Test
    void compileConstantsTCK() {
        Env env = new Env();
        env.addPackage("src/test/java", "io.vertx.codegen.tck.constants");
        MutinyGenerator generator = new MutinyGenerator(env.root());
        List<MutinyGenerator.GeneratorOutput> outputs = generator.generate();
        env.addOutputs(outputs);
        assertThat(env.compile()).isNotEmpty();
    }

}
