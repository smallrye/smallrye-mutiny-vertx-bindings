package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import com.palantir.javapoet.MethodSpec;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import io.smallrye.mutiny.vertx.apigenerator.types.TypeDescriber;

/**
 * These tests are slight different:
 * <p>
 * 1) the generated code is not compiled - because we do not have the ReadStream shim
 * 2) It requires accessing to the Vert.x bare code - so the collection takes more time.
 */
public class ReadWriteStreamTest {

    @Test
    void testInterfaceExtendingReadStreamOfDataObject() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyDataObject.java", """
                package org.acme;

                import io.vertx.codegen.annotations.DataObject;
                import io.vertx.core.json.JsonObject;

                @DataObject
                public class MyDataObject {
                    public MyDataObject() {
                    }
                }
                """)
                .addJavaCode("org.acme", "MyReadStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyReadStream extends ReadStream<MyDataObject> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // We cannot compile as the Mutiny Shim for read stream does not exist

        ShimClass shim = env.getOutputFor("org.acme.MyReadStream").shim();
        assertThat(shim).isNotNull();
        assertThat(shim.getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .contains("io.vertx.mutiny.core.streams.ReadStream<org.acme.MyDataObject>");

    }

    @Test
    void testInterfaceExtendingReadStreamOfPlainObject() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyReadStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyReadStream extends ReadStream<String> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // We cannot compile as the Mutiny Shim for read stream does not exist

        ShimClass shim = env.getOutputFor("org.acme.MyReadStream").shim();
        assertThat(shim).isNotNull();
        assertThat(shim.getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .contains("io.vertx.mutiny.core.streams.ReadStream<java.lang.String>");

    }

    @Test
    void testInterfaceExtendingReadStreamOfVertxGenObject() {
        Env env = new Env();
        env.addJavaCode("org.acme", "Message.java", """
                package org.acme;

                import io.vertx.codegen.annotations.DataObject;
                import io.vertx.core.json.JsonObject;
                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Message<T> {
                    T body();
                }
                """)
                .addJavaCode("org.acme", "MyReadStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyReadStream<T> extends ReadStream<Message<T>> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // We cannot compile as the Mutiny Shim for read stream does not exist

        ShimClass shim = env.getOutputFor("org.acme.MyReadStream").shim();
        assertThat(shim).isNotNull();
        assertThat(shim.getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .contains("io.vertx.mutiny.core.streams.ReadStream<org.acme.mutiny.Message<T>>");
    }

    @Test
    void testInterfaceExtendingWriteStreamOfVertxGen() {
        Env env = new Env();
        env.addJavaCode("org.acme", "Message.java", """
                package org.acme;

                import io.vertx.codegen.annotations.DataObject;
                import io.vertx.core.json.JsonObject;
                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Message<T> {
                    T body();
                }
                """)
                .addJavaCode("org.acme", "MyWriteStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.WriteStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyWriteStream<T> extends WriteStream<Message<T>> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // We cannot compile as the Mutiny Shim for read stream does not exist

        ShimClass shim = env.getOutputFor("org.acme.MyWriteStream").shim();
        assertThat(shim).isNotNull();
        assertThat(shim.getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .contains("io.vertx.mutiny.core.streams.WriteStream<org.acme.mutiny.Message<T>>");
    }

    @Test
    void testInterfaceExtendingWriteStreamOfDataObject() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyDataObject.java", """
                package org.acme;

                import io.vertx.codegen.annotations.DataObject;

                @DataObject
                public class MyDataObject {

                }
                """)
                .addJavaCode("org.acme", "MyWriteStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.WriteStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyWriteStream extends WriteStream<MyDataObject> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // We cannot compile as the Mutiny Shim for read stream does not exist

        ShimClass shim = env.getOutputFor("org.acme.MyWriteStream").shim();
        assertThat(shim).isNotNull();
        assertThat(shim.getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .contains("io.vertx.mutiny.core.streams.WriteStream<org.acme.MyDataObject>");
    }

    @Test
    void testInterfaceExtendingWriteStreamOfPlainObject() {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "MyWriteStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.WriteStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyWriteStream extends WriteStream<String> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // We cannot compile as the Mutiny Shim for read stream does not exist

        ShimClass shim = env.getOutputFor("org.acme.MyWriteStream").shim();
        assertThat(shim).isNotNull();
        assertThat(shim.getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .contains("io.vertx.mutiny.core.streams.WriteStream<java.lang.String>");
    }

    @Test
    void testInterfaceExtendingReadStreamAndWriteStream() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyDataObject.java", """
                package org.acme;

                import io.vertx.codegen.annotations.DataObject;

                @DataObject
                public class MyDataObject {

                }
                """)
                .addJavaCode("org.acme", "MyReadAndWriteStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.WriteStream;
                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyReadAndWriteStream extends ReadStream<MyDataObject>, WriteStream<MyDataObject> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // We cannot compile as the Mutiny Shim for read stream does not exist

        ShimClass shim = env.getOutputFor("org.acme.MyReadAndWriteStream").shim();
        assertThat(shim).isNotNull();
        assertThat(shim.getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .hasSize(2)
                .contains("io.vertx.mutiny.core.streams.WriteStream<org.acme.MyDataObject>",
                        "io.vertx.mutiny.core.streams.ReadStream<org.acme.MyDataObject>");
    }

    @Test
    void testInterfaceExtendingReadStreamAndWriteStreamUsingDifferentTypes() {
        Env env = new Env();
        env.addJavaCode("org.acme", "MyDataObject.java", """
                package org.acme;

                import io.vertx.codegen.annotations.DataObject;

                @DataObject
                public class MyDataObject {

                }
                """)
                .addJavaCode("org.acme", "Message.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.DataObject;
                        import io.vertx.core.json.JsonObject;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Message<T> {
                            T body();
                        }
                        """)
                .addJavaCode("org.acme", "MyReadAndWriteStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.WriteStream;
                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyReadAndWriteStream<T> extends ReadStream<MyDataObject>, WriteStream<Message<T>> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // We cannot compile as the Mutiny Shim for read stream does not exist

        ShimClass shim = env.getOutputFor("org.acme.MyReadAndWriteStream").shim();
        assertThat(shim).isNotNull();
        assertThat(shim.getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .hasSize(2)
                .contains("io.vertx.mutiny.core.streams.WriteStream<org.acme.mutiny.Message<T>>",
                        "io.vertx.mutiny.core.streams.ReadStream<org.acme.MyDataObject>");
    }

    @Test
    void testToMultiWithReadStreamOfString() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyReadStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyReadStream extends ReadStream<String> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        MutinyGenerator.GeneratorOutput output = env.getOutputFor("org.acme.MyReadStream");

        // We cannot compile as the Mutiny Shim for read stream does not exist

        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toMulti");
            assertThat(m.modifiers()).contains(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Multi.class.getName() + "<" + String.class.getName() + ">");
            assertThat(m.annotations().get(0).type().toString()).isEqualTo(CheckReturnValue.class.getName());
            assertThat(m.code().toString())
                    .contains("MultiHelper.toMulti(this.getDelegate()")
                    .contains("return multi;");
        });

        // Also check toBlockingIterable and toBlockingStream

        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toBlockingIterable");
            assertThat(m.modifiers()).doesNotContain(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Iterable.class.getName() + "<" + String.class.getName() + ">");
            assertThat(m.code().toString())
                    .contains("return toMulti().subscribe().asIterable()");
        });

        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toBlockingStream");
            assertThat(m.modifiers()).doesNotContain(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Stream.class.getName() + "<" + String.class.getName() + ">");
            assertThat(m.code().toString())
                    .contains("return toMulti().subscribe().asStream()");
        });

    }

    @Test
    void testToMultiWithReadStreamOfTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "MyReadStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyReadStream<T> extends ReadStream<T> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        MutinyGenerator.GeneratorOutput output = env.getOutputFor("org.acme.MyReadStream");

        // We cannot compile as the Mutiny Shim for read stream does not exist

        // Here is the code generated by the old generator on Vert.x 4 "Row":
        //  @CheckReturnValue
        //  public synchronized Multi<T> toMulti() {
        //    if (multi == null) {
        //      java.util.function.Function<T, T> conv = (java.util.function.Function<T, T>) __typeArg_0.wrap;
        //      multi = io.smallrye.mutiny.vertx.MultiHelper.toMulti(delegate, conv);    }
        //    return multi;
        //  }
        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toMulti");
            assertThat(m.modifiers()).contains(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Multi.class.getName() + "<T>");
            assertThat(m.annotations().get(0).type().toString()).isEqualTo(CheckReturnValue.class.getName());
            assertThat(m.code().toString())
                    .contains("MultiHelper.toMulti(delegate, _conv)")
                    .contains("return multi;");
        });

        // Also check toBlockingIterable and toBlockingStream

        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toBlockingIterable");
            assertThat(m.modifiers()).doesNotContain(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Iterable.class.getName() + "<T>");
            assertThat(m.code().toString())
                    .contains("return toMulti().subscribe().asIterable()");
        });

        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toBlockingStream");
            assertThat(m.modifiers()).doesNotContain(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Stream.class.getName() + "<T>");
            assertThat(m.code().toString())
                    .contains("return toMulti().subscribe().asStream()");
        });
    }

    @Test
    void testToMultiWithReadStreamOfVertxGen() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {
                        }
                        """)
                .addJavaCode("org.acme", "MyReadStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyReadStream extends ReadStream<Refed> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        MutinyGenerator.GeneratorOutput output = env.getOutputFor("org.acme.MyReadStream");
        // We cannot compile as the Mutiny Shim for read stream does not exist

        // Here is the code generated by the old generator on Vert.x 4:
        //  @CheckReturnValue
        //  public synchronized Multi<io.vertx.mutiny.amqp.AmqpMessage> toMulti() {
        //    if (multi == null) {
        //      java.util.function.Function<io.vertx.amqp.AmqpMessage, io.vertx.mutiny.amqp.AmqpMessage> conv = io.vertx.mutiny.amqp.AmqpMessage::newInstance;
        //      multi = io.smallrye.mutiny.vertx.MultiHelper.toMulti(delegate, conv);    }
        //    return multi;
        //  }
        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toMulti");
            assertThat(m.modifiers()).contains(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Multi.class.getName() + "<org.acme.mutiny.Refed>");
            assertThat(m.annotations().get(0).type().toString()).isEqualTo(CheckReturnValue.class.getName());
            assertThat(m.code().toString())
                    .contains("Refed::newInstance")
                    .contains("return multi;");
        });

        // Also check toBlockingIterable and toBlockingStream

        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toBlockingIterable");
            assertThat(m.modifiers()).doesNotContain(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Iterable.class.getName() + "<org.acme.mutiny.Refed>");
            assertThat(m.code().toString())
                    .contains("return toMulti().subscribe().asIterable()");
        });

        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toBlockingStream");
            assertThat(m.modifiers()).doesNotContain(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Stream.class.getName() + "<org.acme.mutiny.Refed>");
            assertThat(m.code().toString())
                    .contains("return toMulti().subscribe().asStream()");
        });
    }

    @Test
    void testToMultiWithReadStreamOfVertxGenWithATypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed<I> {
                        }
                        """)
                .addJavaCode("org.acme", "MyReadStream.java", """
                        package org.acme;

                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyReadStream<I> extends ReadStream<Refed<I>> {
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        MutinyGenerator.GeneratorOutput output = env.getOutputFor("org.acme.MyReadStream");

        // We cannot compile as the Mutiny Shim for read stream does not exist

        // Here is the code generated by the old generator on Vert.x 4:
        //  @CheckReturnValue
        //  public synchronized Multi<io.vertx.mutiny.amqp.AmqpMessage> toMulti() {
        //    if (multi == null) {
        //      java.util.function.Function<io.vertx.amqp.AmqpMessage, io.vertx.mutiny.amqp.AmqpMessage> conv = io.vertx.mutiny.amqp.AmqpMessage::newInstance;
        //      multi = io.smallrye.mutiny.vertx.MultiHelper.toMulti(delegate, conv);    }
        //    return multi;
        //  }
        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toMulti");
            assertThat(m.modifiers()).contains(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Multi.class.getName() + "<org.acme.mutiny.Refed<I>>");
            assertThat(m.annotations().get(0).type().toString()).isEqualTo(CheckReturnValue.class.getName());
            assertThat(m.code().toString())
                    .contains("Refed::newInstance")// Would that really work when using Type Parameter?
                    .contains("return multi;");
        });

        // Also check toBlockingIterable and toBlockingStream

        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toBlockingIterable");
            assertThat(m.modifiers()).doesNotContain(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Iterable.class.getName() + "<org.acme.mutiny.Refed<I>>");
            assertThat(m.code().toString())
                    .contains("return toMulti().subscribe().asIterable()");
        });

        assertThat(output.javaFile().typeSpec().methodSpecs()).anySatisfy(m -> {
            assertThat(m.name()).isEqualTo("toBlockingStream");
            assertThat(m.modifiers()).doesNotContain(Modifier.SYNCHRONIZED);
            assertThat(m.returnType().toString()).isEqualTo(Stream.class.getName() + "<org.acme.mutiny.Refed<I>>");
            assertThat(m.code().toString())
                    .contains("return toMulti().subscribe().asStream()");
        });
    }

    @Test
    void testMethodsAcceptingAReadStream() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface.java",
                        """
                                package org.acme;

                                import io.vertx.core.streams.ReadStream;
                                import io.vertx.codegen.annotations.VertxGen;
                                import io.vertx.core.Future;
                                import io.vertx.codegen.annotations.Nullable;

                                @VertxGen
                                public interface MyInterface {
                                    Future<Refed> acceptReadStreamOfVGAndReturnFutureOfVG(ReadStream<Refed> stream);
                                    Future<Refed> acceptReadStreamOfStringAndReturnFutureOfVG(ReadStream<String> stream);

                                    // Just there to verify generation.
                                    Future<Refed> acceptReadStreamOfVGAndReturnFutureOfVGNullable(@Nullable ReadStream<Refed> stream);
                                    Future<Refed> acceptReadStreamOfStringAndReturnFutureOfVGNullable(@Nullable ReadStream<String> stream);

                                    Future<String> acceptReadStreamOfVGAndReturnFutureOfString(ReadStream<Refed> stream);
                                    Future<String> acceptReadStreamOfStringAndReturnFutureOfString(ReadStream<String> stream);

                                    Refed acceptReadStreamOfVGAndReturnVG(ReadStream<Refed> stream);
                                    Refed acceptReadStreamOfStringAndReturnVG(ReadStream<String> stream);

                                    String acceptReadStreamOfVGAndReturnString(ReadStream<Refed> stream);
                                    String acceptReadStreamOfStringAndReturnString(ReadStream<String> stream);
                                }
                                """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // Check that all methods have been generated
        final String readStreamOfString = "io.vertx.mutiny.core.streams.ReadStream<java.lang.String>";
        final String readStreamOfVG = "io.vertx.mutiny.core.streams.ReadStream<org.acme.mutiny.Refed>";
        final String publisherOfVG = "java.util.concurrent.Flow.Publisher<org.acme.mutiny.Refed>";
        final String publisherOfString = "java.util.concurrent.Flow.Publisher<java.lang.String>";
        final String uniOfVG = Uni.class.getName() + "<org.acme.mutiny.Refed>";
        final String uniOfString = Uni.class.getName() + "<java.lang.String>";
        final String VG = "org.acme.mutiny.Refed";

        List<MethodSpec> methods = env.getOutputFor("org.acme.MyInterface").javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().startsWith("accept")).toList();

        assertThat(methods)
                .anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnFutureOfVG", uniOfVG, readStreamOfVG));
        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnFutureOfVG", uniOfVG, publisherOfVG));

        assertThat(methods)
                .anySatisfy(m -> assertMethod(m, "acceptReadStreamOfStringAndReturnFutureOfVG", uniOfVG, readStreamOfString));
        assertThat(methods)
                .anySatisfy(m -> assertMethod(m, "acceptReadStreamOfStringAndReturnFutureOfVG", uniOfVG, publisherOfString));

        assertThat(methods)
                .anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnFutureOfString", uniOfString, readStreamOfVG));
        assertThat(methods)
                .anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnFutureOfString", uniOfString, publisherOfVG));

        assertThat(methods).anySatisfy(
                m -> assertMethod(m, "acceptReadStreamOfStringAndReturnFutureOfString", uniOfString, readStreamOfString));
        assertThat(methods).anySatisfy(
                m -> assertMethod(m, "acceptReadStreamOfStringAndReturnFutureOfString", uniOfString, readStreamOfString));

        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnVG", VG, readStreamOfVG));
        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnVG", VG, publisherOfVG));

        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfStringAndReturnVG", VG, readStreamOfString));
        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfStringAndReturnVG", VG, publisherOfString));

        assertThat(methods).anySatisfy(
                m -> assertMethod(m, "acceptReadStreamOfVGAndReturnString", java.lang.String.class.getName(), readStreamOfVG));
        assertThat(methods).anySatisfy(
                m -> assertMethod(m, "acceptReadStreamOfVGAndReturnString", java.lang.String.class.getName(), publisherOfVG));

        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfStringAndReturnString",
                java.lang.String.class.getName(), readStreamOfString));
        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfStringAndReturnString",
                java.lang.String.class.getName(), publisherOfString));
    }

    @Test
    void testMethodsAcceptingAReadStreamOfVertxGenUsingTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed<I> {
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;

                        @VertxGen
                        public interface MyInterface<I> {
                            Future<Refed<I>> acceptReadStreamOfVGAndReturnFutureOfVG(ReadStream<Refed<I>> stream);
                            Future<String> acceptReadStreamOfVGAndReturnFutureOfString(ReadStream<Refed<I>> stream);
                            Refed<I> acceptReadStreamOfVGAndReturnVG(ReadStream<Refed<I>> stream);
                            String acceptReadStreamOfVGAndReturnString(ReadStream<Refed<I>> stream);
                            Future<String> acceptMoreReturnFutureOfString(String s, ReadStream<Refed<I>> stream);
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // Check that all methods have been generated
        final String readStreamOfVG = "io.vertx.mutiny.core.streams.ReadStream<org.acme.mutiny.Refed<I>>";
        final String publisherOfVG = "java.util.concurrent.Flow.Publisher<org.acme.mutiny.Refed<I>>";
        final String uniOfVG = Uni.class.getName() + "<org.acme.mutiny.Refed<I>>";
        final String uniOfString = Uni.class.getName() + "<java.lang.String>";
        final String VG = "org.acme.mutiny.Refed<I>";

        List<MethodSpec> methods = env.getOutputFor("org.acme.MyInterface").javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().startsWith("accept")).toList();

        assertThat(methods)
                .anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnFutureOfVG", uniOfVG, readStreamOfVG));
        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnFutureOfVG", uniOfVG, publisherOfVG));

        assertThat(methods)
                .anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnFutureOfString", uniOfString, readStreamOfVG));
        assertThat(methods)
                .anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnFutureOfString", uniOfString, publisherOfVG));

        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnVG", VG, readStreamOfVG));
        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfVGAndReturnVG", VG, publisherOfVG));

        assertThat(methods).anySatisfy(
                m -> assertMethod(m, "acceptReadStreamOfVGAndReturnString", java.lang.String.class.getName(), readStreamOfVG));
        assertThat(methods).anySatisfy(
                m -> assertMethod(m, "acceptReadStreamOfVGAndReturnString", java.lang.String.class.getName(), publisherOfVG));

        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptMoreReturnFutureOfString",
                uniOfString, java.lang.String.class.getName(), readStreamOfVG));
        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptMoreReturnFutureOfString",
                uniOfString, java.lang.String.class.getName(), publisherOfVG));
    }

    @Test
    void testStaticMethodsAcceptingAReadStream() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;

                        @VertxGen
                        public interface MyInterface {
                            static Future<Refed> acceptReadStreamOfVGAndReturnFutureOfVG(ReadStream<Refed> stream) {
                                   return null;
                            }
                            static Future<Refed> acceptReadStreamOfStringAndReturnFutureOfVG(ReadStream<String> stream) {
                                   return null;
                            }

                            static  Future<String> acceptReadStreamOfVGAndReturnFutureOfString(ReadStream<Refed> stream) {
                                   return null;
                            }
                            static  Future<String> acceptReadStreamOfStringAndReturnFutureOfString(ReadStream<String> stream) {
                                   return null;
                            }

                            static  Refed acceptReadStreamOfVGAndReturnVG(ReadStream<Refed> stream) {
                                   return null;
                            }
                            static  Refed acceptReadStreamOfStringAndReturnVG(ReadStream<String> stream) {
                                   return null;
                            }

                            static  String acceptReadStreamOfVGAndReturnString(ReadStream<Refed> stream) {
                                   return null;
                            }
                            static String acceptReadStreamOfStringAndReturnString(ReadStream<String> stream) {
                                   return null;
                            }

                            static  Future<String> acceptMoreReturnFutureOfString(ReadStream<String> stream, String s) {
                                   return null;
                            }

                            static  Refed acceptMorefVGAndReturnVG(String s, ReadStream<Refed> stream) {
                                   return null;
                            }
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // Check that all methods have been generated
        final String readStreamOfString = "io.vertx.mutiny.core.streams.ReadStream<java.lang.String>";
        final String readStreamOfVG = "io.vertx.mutiny.core.streams.ReadStream<org.acme.mutiny.Refed>";
        final String publisherOfVG = "java.util.concurrent.Flow.Publisher<org.acme.mutiny.Refed>";
        final String publisherOfString = "java.util.concurrent.Flow.Publisher<java.lang.String>";
        final String uniOfVG = Uni.class.getName() + "<org.acme.mutiny.Refed>";
        final String uniOfString = Uni.class.getName() + "<java.lang.String>";
        final String VG = "org.acme.mutiny.Refed";

        List<MethodSpec> methods = env.getOutputFor("org.acme.MyInterface").javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().startsWith("accept")).toList();

        assertThat(methods)
                .anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnFutureOfVG", uniOfVG, readStreamOfVG));
        assertThat(methods)
                .anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnFutureOfVG", uniOfVG, publisherOfVG));

        assertThat(methods).anySatisfy(
                m -> assertStaticMethod(m, "acceptReadStreamOfStringAndReturnFutureOfVG", uniOfVG, readStreamOfString));
        assertThat(methods).anySatisfy(
                m -> assertStaticMethod(m, "acceptReadStreamOfStringAndReturnFutureOfVG", uniOfVG, publisherOfString));

        assertThat(methods).anySatisfy(
                m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnFutureOfString", uniOfString, readStreamOfVG));
        assertThat(methods).anySatisfy(
                m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnFutureOfString", uniOfString, publisherOfVG));

        assertThat(methods).anySatisfy(
                m -> assertStaticMethod(m, "acceptReadStreamOfStringAndReturnFutureOfString", uniOfString, readStreamOfString));
        assertThat(methods).anySatisfy(
                m -> assertStaticMethod(m, "acceptReadStreamOfStringAndReturnFutureOfString", uniOfString, readStreamOfString));

        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnVG", VG, readStreamOfVG));
        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnVG", VG, publisherOfVG));

        assertThat(methods)
                .anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfStringAndReturnVG", VG, readStreamOfString));
        assertThat(methods)
                .anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfStringAndReturnVG", VG, publisherOfString));

        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnString",
                java.lang.String.class.getName(), readStreamOfVG));
        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnString",
                java.lang.String.class.getName(), publisherOfVG));

        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfStringAndReturnString",
                java.lang.String.class.getName(), readStreamOfString));
        assertThat(methods).anySatisfy(m -> assertMethod(m, "acceptReadStreamOfStringAndReturnString",
                java.lang.String.class.getName(), publisherOfString));

        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptMoreReturnFutureOfString",
                uniOfString, readStreamOfString, java.lang.String.class.getName()));
        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptMoreReturnFutureOfString",
                uniOfString, publisherOfString, java.lang.String.class.getName()));

        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptMorefVGAndReturnVG",
                VG, java.lang.String.class.getName(), readStreamOfVG));
        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptMorefVGAndReturnVG",
                VG, java.lang.String.class.getName(), publisherOfVG));
    }

    @Test
    void testStaticMethodsAcceptingAReadStreamOfVertxGenUsingTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed<I> {
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Future;

                        @VertxGen
                        public interface MyInterface {
                            static <I> Future<Refed<I>> acceptReadStreamOfVGAndReturnFutureOfVG(ReadStream<Refed<I>> stream) {
                                return null;
                            }
                            static <I> Future<String> acceptReadStreamOfVGAndReturnFutureOfString(ReadStream<Refed<I>> stream) {
                                return null;
                            }
                            static <I> Refed<I> acceptReadStreamOfVGAndReturnVG(ReadStream<Refed<I>> stream) {
                                return null;
                            }
                            static <I> String acceptReadStreamOfVGAndReturnString(ReadStream<Refed<I>> stream) {
                                return null;
                            }
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        // Check that all methods have been generated
        final String readStreamOfVG = "io.vertx.mutiny.core.streams.ReadStream<org.acme.mutiny.Refed<I>>";
        final String publisherOfVG = "java.util.concurrent.Flow.Publisher<org.acme.mutiny.Refed<I>>";
        final String uniOfVG = Uni.class.getName() + "<org.acme.mutiny.Refed<I>>";
        final String uniOfString = Uni.class.getName() + "<java.lang.String>";
        final String VG = "org.acme.mutiny.Refed<I>";

        List<MethodSpec> methods = env.getOutputFor("org.acme.MyInterface").javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().startsWith("accept")).toList();

        assertThat(methods)
                .anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnFutureOfVG", uniOfVG, readStreamOfVG));
        assertThat(methods)
                .anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnFutureOfVG", uniOfVG, publisherOfVG));

        assertThat(methods).anySatisfy(
                m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnFutureOfString", uniOfString, readStreamOfVG));
        assertThat(methods).anySatisfy(
                m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnFutureOfString", uniOfString, publisherOfVG));

        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnVG", VG, readStreamOfVG));
        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnVG", VG, publisherOfVG));

        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnString",
                java.lang.String.class.getName(), readStreamOfVG));
        assertThat(methods).anySatisfy(m -> assertStaticMethod(m, "acceptReadStreamOfVGAndReturnString",
                java.lang.String.class.getName(), publisherOfVG));
    }

    @Test
    void testToSubscriberMethodGenerationWithWriteStreamOfString() {
        Env env = new Env()
                .addModuleGen("org.acme", "my-module")
                .addJavaCode("org.acme", "MyWriteStream", """

                        package org.acme;

                        import io.vertx.core.streams.WriteStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyWriteStream extends WriteStream<String> {

                        }
                        """);
        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        List<MethodSpec> methods = env.getOutputFor("org.acme.MyWriteStream").javaFile().typeSpec().methodSpecs();
        assertThat(methods)
                .anySatisfy(m -> assertMethod(m, "toSubscriber", "java.util.concurrent.Flow.Subscriber<java.lang.String>"));
    }

    @Test
    void testToSubscriberMethodGenerationWithWriteStreamOfVG() {
        Env env = new Env()
                .addModuleGen("org.acme", "my-module")
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {
                        }
                        """)
                .addJavaCode("org.acme", "MyWriteStream", """

                        package org.acme;

                        import io.vertx.core.streams.WriteStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyWriteStream extends WriteStream<Refed> {

                        }
                        """);
        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        List<MethodSpec> methods = env.getOutputFor("org.acme.MyWriteStream").javaFile().typeSpec().methodSpecs();
        assertThat(methods).anySatisfy(
                m -> assertMethod(m, "toSubscriber", "java.util.concurrent.Flow.Subscriber<org.acme.mutiny.Refed>"));
    }

    @Test
    void testToSubscriberMethodGenerationWithWriteStreamOfVGWithTypeParameter() {
        Env env = new Env()
                .addModuleGen("org.acme", "my-module")
                .addJavaCode("org.acme", "Refed", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed<I> {
                        }
                        """)
                .addJavaCode("org.acme", "MyWriteStream", """

                        package org.acme;

                        import io.vertx.core.streams.WriteStream;
                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyWriteStream<I> extends WriteStream<Refed<I>> {

                        }
                        """);
        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        List<MethodSpec> methods = env.getOutputFor("org.acme.MyWriteStream").javaFile().typeSpec().methodSpecs();
        assertThat(methods).anySatisfy(
                m -> assertMethod(m, "toSubscriber", "java.util.concurrent.Flow.Subscriber<org.acme.mutiny.Refed<I>>"));
    }

    @Test
    void testWhenInterfaceExtendReadStreamOfVertxGenWithoutTypeParameter() {
        Env env = new Env();
        env.addJavaCode("org.acme", "Refed", """
                package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface Refed {
                        }
                """)
                .addJavaCode("org.acme", "MyReadStream", """
                        package org.acme;

                        import io.vertx.core.streams.ReadStream;
                        import io.vertx.codegen.annotations.VertxGen;
                        @VertxGen
                        public interface MyReadStream extends ReadStream<Refed> {

                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        MethodSpec pipe = env.getOutputFor("org.acme.MyReadStream").javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().equals("pipe"))
                .findFirst().orElseThrow();
        assertThat(pipe.toString()).doesNotContain("__typeArg_");
    }

    @Test
    public void testNestedTypeParameter() {
        Env env = new Env()
                .addJavaCode("org.acme", "ChangeStreamDocument", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        public interface ChangeStreamDocument<T> {
                            T getFullDocument();
                        }
                        """)
                .addJavaCode("org.acme", "MyService", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.streams.ReadStream;

                        @VertxGen
                        public interface MyService {
                            ReadStream<ChangeStreamDocument<String>> watch();

                            <T> ReadStream<ChangeStreamDocument<T>> foo(Class<T> clazz);
                        }
                        """)
                .addModuleGen("org.acme", "my-module");
        MutinyGenerator generator = new MutinyGenerator(env.root(), "my-module", Paths.get("target/vertx-core-sources"));
        env.addOutputs(generator.generate());

        MethodSpec method = env.getOutputFor("org.acme.MyService").javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().startsWith("watch")).findFirst().orElseThrow();
        assertThat(method.returnType().toString())
                .isEqualTo("io.vertx.mutiny.core.streams.ReadStream<org.acme.ChangeStreamDocument<java.lang.String>>");

        method = env.getOutputFor("org.acme.MyService").javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().startsWith("foo")).findFirst().orElseThrow();
        assertThat(method.returnType().toString())
                .isEqualTo("io.vertx.mutiny.core.streams.ReadStream<org.acme.ChangeStreamDocument<T>>");

    }

    private void assertMethod(MethodSpec method, String name, String returnType, String... parameterTypes) {
        assertThat(method.name()).isEqualTo(name);
        assertThat(method.returnType().toString()).isEqualTo(returnType);
        assertThat(method.parameters()).hasSize(parameterTypes.length);
        for (int i = 0; i < parameterTypes.length; i++) {
            assertThat(method.parameters().get(i).type().toString()).isEqualTo(parameterTypes[i]);
        }
    }

    private void assertStaticMethod(MethodSpec method, String name, String returnType, String... parameterTypes) {
        assertThat(method.modifiers()).contains(Modifier.STATIC);
        assertMethod(method, name, returnType, parameterTypes);
    }

}
